import { type FormEvent, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { PageHeader } from "@/components/layout/PageHeader";
import { BatchSendDialog } from "@/components/delivery/BatchSendDialog";
import { DeliveryManagerModal } from "@/components/delivery/DeliveryManagerModal";
import { PressedCardDialog, type PressedInvitation } from "@/components/cards/PressedCardGrid";
import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { NativeSelect } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { apiFetch, eventsListPath, readError, readJson } from "@/lib/api";
import { EVENT_TYPES } from "@/lib/catalog";
import { setActiveEventId, syncActiveEventIdOrFirst } from "@/lib/session";

interface Guest {
  id: string;
  eventId: string;
  fullName: string;
  phone?: string;
  email?: string;
  admissionType?: "SINGLE" | "DOUBLE";
  admissionLimit?: number;
}

interface EventItem {
  id: string;
  eventName: string;
  currentTemplateId?: string | null;
}

function defaultEventDateInput(): string {
  const date = new Date();
  date.setDate(date.getDate() + 14);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

interface ImportRow {
  rowNumber: number;
  fullName: string;
  phone: string;
  email: string;
  valid: boolean;
  duplicate: boolean;
  errors: string[];
}

interface ImportPreview {
  eventId: string;
  fileName: string;
  deliveryChannel: string;
  totalRows: number;
  validCount: number;
  invalidCount: number;
  duplicateCount: number;
  rows: ImportRow[];
}

export function GuestManagerPage() {
  const navigate = useNavigate();
  const [events, setEvents] = useState<EventItem[]>([]);
  const [selectedEventId, setSelectedEventId] = useState("");
  const [guests, setGuests] = useState<Guest[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingGuest, setEditingGuest] = useState<Guest | null>(null);
  const [formData, setFormData] = useState({ fullName: "", phone: "", email: "", admissionType: "SINGLE" });
  const [formError, setFormError] = useState("");
  const [pageError, setPageError] = useState("");
  const [deletingGuestId, setDeletingGuestId] = useState<string | null>(null);
  const [deliveryModalGuest, setDeliveryModalGuest] = useState<Guest | null>(null);
  const [deliveryModalInvitationId, setDeliveryModalInvitationId] = useState("");
  const [isDeliveryModalOpen, setIsDeliveryModalOpen] = useState(false);
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [importStep, setImportStep] = useState(1);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [deliveryChannel, setDeliveryChannel] = useState("BOTH");
  const [isUploading, setIsUploading] = useState(false);
  const [previewData, setPreviewData] = useState<ImportPreview | null>(null);
  const [previewFilter, setPreviewFilter] = useState<"ALL" | "VALID_ONLY" | "ERRORS_ONLY">("ALL");
  const [importResult, setImportResult] = useState<{ importedCount: number; skippedCount: number } | null>(null);
  const [isEventModalOpen, setIsEventModalOpen] = useState(false);
  const [newEventName, setNewEventName] = useState("");
  const [newEventType, setNewEventType] = useState("WEDDING");
  const [newEventDate, setNewEventDate] = useState("");
  const [newEventTime, setNewEventTime] = useState("18:00");
  const [newEventVenue, setNewEventVenue] = useState("");
  const [eventFormError, setEventFormError] = useState("");
  const [savingEvent, setSavingEvent] = useState(false);
  const [viewingCard, setViewingCard] = useState<PressedInvitation | null>(null);
  const [isBatchSendOpen, setIsBatchSendOpen] = useState(false);

  const handleViewCard = async (guest: Guest) => {
    try {
      const res = await apiFetch(`/invitations/guest/${guest.id}`);
      if (!res.ok) {
        setPageError(await readError(res));
        return;
      }
      const invs = await readJson<{ id: string; guestName?: string }[]>(res);
      if (!invs.length) {
        setPageError("Generate this guest’s card first, then open it here.");
        return;
      }
      setPageError("");
      setViewingCard({ id: invs[0].id, guestName: invs[0].guestName || guest.fullName });
    } catch {
      setPageError("Could not load this guest’s card.");
    }
  };

  const handleOpenDeliveryModal = async (guest: Guest) => {
    setDeliveryModalGuest(guest);
    try {
      const res = await apiFetch(`/invitations/guest/${guest.id}`);
      if (res.ok) {
        const invs = await readJson<{ id: string }[]>(res);
        if (invs && invs.length > 0) {
          setDeliveryModalInvitationId(invs[0].id);
          setIsDeliveryModalOpen(true);
          return;
        }
      }

      const currentTemplateId = events.find((event) => event.id === guest.eventId)?.currentTemplateId;
      if (!currentTemplateId) {
        setPageError("Set this event’s current template first, then send invitations.");
        return;
      }

      const createRes = await apiFetch("/invitations", {
        method: "POST",
        body: JSON.stringify({
          eventId: guest.eventId,
          guestId: guest.id,
          templateId: currentTemplateId,
          recipientEmail: guest.email,
          recipientPhone: guest.phone,
        }),
      });

      if (createRes.ok) {
        const newInv = await readJson<{ id: string }>(createRes);
        setDeliveryModalInvitationId(newInv.id);
        setIsDeliveryModalOpen(true);
      } else {
        setPageError("Could not create an invitation for this guest.");
      }
    } catch {
      setPageError("Could not reach the delivery service.");
    }
  };

  useEffect(() => {
    fetchEvents();
  }, []);

  useEffect(() => {
    if (selectedEventId) fetchGuests();
  }, [selectedEventId]);

  const fetchEvents = async () => {
    try {
      const res = await apiFetch(eventsListPath());
      if (res.ok) {
        const data = await readJson<EventItem[]>(res);
        setEvents(data);
        setSelectedEventId(syncActiveEventIdOrFirst(data.map((event) => event.id)) ?? "");
      } else {
        setPageError(await readError(res));
      }
    } catch {
      setPageError("Could not load events.");
    }
  };

  const fetchGuests = async () => {
    if (!selectedEventId) return;
    setIsLoading(true);
    try {
      const url = searchQuery.trim()
        ? `/guests/search?eventId=${selectedEventId}&query=${encodeURIComponent(searchQuery.trim())}`
        : `/guests/event/${selectedEventId}`;
      const res = await apiFetch(url);
      if (res.ok) setGuests(await readJson<Guest[]>(res));
    } catch {
      setPageError("Could not load guests.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleSaveGuest = async (e: FormEvent) => {
    e.preventDefault();
    setFormError("");
    if (!selectedEventId) {
      setFormError("Create an event first, then add this guest to it.");
      return;
    }
    if (!formData.fullName.trim()) {
      setFormError("Full name is required");
      return;
    }
    if (!formData.phone.trim() && !formData.email.trim()) {
      setFormError("Enter a phone number or an email.");
      return;
    }
    try {
      const url = editingGuest ? `/guests/${editingGuest.id}` : "/guests";
      const res = await apiFetch(url, {
        method: editingGuest ? "PUT" : "POST",
        body: JSON.stringify({
          eventId: selectedEventId,
          fullName: formData.fullName.trim(),
          phone: formData.phone.trim() || null,
          email: formData.email.trim() || null,
          admissionType: formData.admissionType,
        }),
      });
      if (res.ok) {
        setIsModalOpen(false);
        toast.success(editingGuest ? "Saved changes" : "Guest added");
        fetchGuests();
      } else {
        setFormError(await readError(res));
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "Could not save this guest.");
    }
  };

  const handleCreateEvent = async (e: FormEvent) => {
    e.preventDefault();
    setEventFormError("");
    if (!newEventName.trim()) {
      setEventFormError("Event name is required.");
      return;
    }
    setSavingEvent(true);
    try {
      const datePart = newEventDate || defaultEventDateInput();
      const timePart = newEventTime || "18:00";
      const res = await apiFetch("/events", {
        method: "POST",
        body: JSON.stringify({
          eventName: newEventName.trim(),
          eventType: newEventType,
          status: "ACTIVE",
          venue: newEventVenue.trim() || "To be confirmed",
          eventDate: `${datePart}T${timePart}:00`,
        }),
      });
      if (!res.ok) {
        setEventFormError(await readError(res));
        return;
      }
      const created = await readJson<EventItem>(res);
      setEvents((current) => [created, ...current.filter((event) => event.id !== created.id)]);
      setSelectedEventId(created.id);
      setActiveEventId(created.id);
      setIsEventModalOpen(false);
      setNewEventName("");
      setNewEventDate("");
      setNewEventTime("18:00");
      setNewEventVenue("");
      setPageError("");
      toast.success("Event created");
    } catch (err) {
      setEventFormError(err instanceof Error ? err.message : "Could not create the event.");
    } finally {
      setSavingEvent(false);
    }
  };

  const handleDeleteGuest = async () => {
    if (!deletingGuestId) return;
    try {
      const res = await apiFetch(`/guests/${deletingGuestId}`, { method: "DELETE" });
      if (res.ok) {
        setDeletingGuestId(null);
        fetchGuests();
      }
    } catch {
      setPageError("Could not delete guest.");
    }
  };

  const openNewEvent = () => {
    setNewEventName("");
    setNewEventType("WEDDING");
    setNewEventDate(defaultEventDateInput());
    setNewEventTime("18:00");
    setNewEventVenue("");
    setEventFormError("");
    setIsEventModalOpen(true);
  };

  const handleOpenImportWizard = () => {
    setImportStep(1);
    setSelectedFile(null);
    setPreviewData(null);
    setImportResult(null);
    setDeliveryChannel("BOTH");
    setIsImportModalOpen(true);
  };

  const handleUploadAndPreview = async () => {
    if (!selectedFile || !selectedEventId) return;
    setIsUploading(true);
    try {
      const body = new FormData();
      body.append("file", selectedFile);
      body.append("eventId", selectedEventId);
      body.append("deliveryChannel", deliveryChannel);
      const res = await apiFetch("/guests/import/preview", { method: "POST", body });
      if (res.ok) {
        setPreviewData(await readJson<ImportPreview>(res));
        setImportStep(2);
      } else {
        setPageError("Could not parse that file. Use CSV or Excel.");
      }
    } catch {
      setPageError("Could not upload the file.");
    } finally {
      setIsUploading(false);
    }
  };

  const handleConfirmImport = async () => {
    if (!previewData || !selectedEventId) return;
    setIsUploading(true);
    try {
      const res = await apiFetch("/guests/import/confirm", {
        method: "POST",
        body: JSON.stringify({
          eventId: selectedEventId,
          deliveryChannel,
          rowsToImport: previewData.rows.filter((r) => r.valid),
        }),
      });
      if (res.ok) {
        const data = await readJson<{ importedCount: number; skippedCount: number }>(res);
        setImportResult({ importedCount: data.importedCount, skippedCount: data.skippedCount });
        setImportStep(3);
        fetchGuests();
      }
    } catch {
      setPageError("Could not confirm import.");
    } finally {
      setIsUploading(false);
    }
  };

  const filteredPreviewRows =
    previewData?.rows.filter((r) => {
      if (previewFilter === "VALID_ONLY") return r.valid;
      if (previewFilter === "ERRORS_ONLY") return !r.valid;
      return true;
    }) || [];

  return (
    <div>
      {pageError ? (
        <Alert variant="destructive" className="mb-6">
          {pageError}
        </Alert>
      ) : null}

      <PageHeader title="Guests" description="Invitees, contact details, and list import.">
        <Button variant="outline" onClick={() => navigate("/admin/scan")}>
          Scanner
        </Button>
        <Button
          variant="outline"
          onClick={() => {
            if (!selectedEventId) {
              setPageError("Create an event first, then add guests.");
              return;
            }
            if (guests.length === 0) {
              setPageError("Add guests before sending.");
              return;
            }
            setPageError("");
            setIsBatchSendOpen(true);
          }}
        >
          Send all
        </Button>
        <Button variant="outline" onClick={handleOpenImportWizard}>
          Import list
        </Button>
        <Button
          onClick={() => {
            if (!selectedEventId) {
              setPageError("Create an event first, then add guests to it.");
              openNewEvent();
              return;
            }
            setEditingGuest(null);
            setFormData({ fullName: "", phone: "", email: "", admissionType: "SINGLE" });
            setFormError("");
            setIsModalOpen(true);
          }}
        >
          Add guest
        </Button>
      </PageHeader>

      <Card className="mb-6 p-6">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            fetchGuests();
          }}
          className="flex flex-col gap-4 md:flex-row md:items-end"
        >
          <div className="min-w-52 flex-1 space-y-2">
            <Label htmlFor="guest-event">Event</Label>
            <div className="flex gap-2">
              <NativeSelect
                id="guest-event"
                className="min-w-0 flex-1"
                value={selectedEventId}
                onChange={(e) => {
                  setSelectedEventId(e.target.value);
                  setActiveEventId(e.target.value || null);
                }}
              >
                {events.length === 0 ? <option value="">No events yet</option> : null}
                {events.map((ev) => (
                  <option key={ev.id} value={ev.id}>
                    {ev.eventName}
                  </option>
                ))}
              </NativeSelect>
              <Button type="button" variant="outline" onClick={openNewEvent}>
                New event
              </Button>
            </div>
          </div>
          <div className="flex-2 min-w-64 flex-1 space-y-2">
            <Label htmlFor="guest-search">Search</Label>
            <Input
              id="guest-search"
              placeholder="Name, phone, or email"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <Button type="submit">Search</Button>
        </form>
      </Card>

      <Card className="overflow-hidden p-0">
        {isLoading ? (
          <p className="p-12 text-center text-sm text-muted-foreground">Loading directory…</p>
        ) : guests.length === 0 ? (
          <div className="p-12 text-center">
            <p className="font-medium">No guests found</p>
            <p className="mt-2 text-sm text-muted-foreground">
              {events.length === 0
                ? "Create an event first, then add guests to the list."
                : "Add a guest or import a spreadsheet to fill the list."}
            </p>
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Admission</TableHead>
                <TableHead>Phone</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Delivery</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {guests.map((g) => {
                const hasPhone = !!g.phone;
                const hasEmail = !!g.email;
                return (
                  <TableRow key={g.id}>
                    <TableCell className="font-medium">{g.fullName}</TableCell>
                    <TableCell>
                      <Badge variant={g.admissionType === "DOUBLE" ? "secondary" : "outline"}>
                        {g.admissionType === "DOUBLE" ? "Double · 2" : "Single · 1"}
                      </Badge>
                    </TableCell>
                    <TableCell className="font-mono text-xs">{g.phone || "—"}</TableCell>
                    <TableCell>{g.email || "—"}</TableCell>
                    <TableCell>
                      <div className="flex flex-wrap gap-1">
                        {hasPhone ? <Badge variant="success">SMS / WhatsApp</Badge> : null}
                        {hasEmail ? <Badge variant="secondary">Email</Badge> : null}
                        {!hasPhone && !hasEmail ? <Badge variant="destructive">Incomplete</Badge> : null}
                      </div>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <Button variant="outline" size="sm" onClick={() => handleViewCard(g)}>
                          View card
                        </Button>
                        <Button variant="outline" size="sm" onClick={() => handleOpenDeliveryModal(g)}>
                          Send
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            setEditingGuest(g);
                            setFormData({
                              fullName: g.fullName || "",
                              phone: g.phone || "",
                              email: g.email || "",
                              admissionType: g.admissionType === "DOUBLE" ? "DOUBLE" : "SINGLE",
                            });
                            setFormError("");
                            setIsModalOpen(true);
                          }}
                        >
                          Edit
                        </Button>
                        <Button variant="ghost" size="sm" onClick={() => setDeletingGuestId(g.id)}>
                          Delete
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}
      </Card>

      <Sheet open={isModalOpen} onOpenChange={setIsModalOpen}>
        <SheetContent>
          <form onSubmit={handleSaveGuest} className="flex h-full flex-col">
            <SheetHeader>
              <SheetTitle>{editingGuest ? "Edit guest" : "Add guest"}</SheetTitle>
              <SheetDescription>
                Name, admission type, and at least one contact method. The event’s current card is applied automatically.
              </SheetDescription>
            </SheetHeader>
            <div className="grid flex-1 gap-4 overflow-y-auto px-6 py-4">
            {formError ? (
              <Alert variant="destructive">
                {formError}
              </Alert>
            ) : null}
              <div className="space-y-2">
                <Label htmlFor="full-name">Full name</Label>
                <Input
                  id="full-name"
                  required
                  placeholder="Jordan Blake"
                  value={formData.fullName}
                  onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="admission-type">Admission</Label>
                <NativeSelect
                  id="admission-type"
                  value={formData.admissionType}
                  onChange={(e) => setFormData({ ...formData, admissionType: e.target.value })}
                >
                  <option value="SINGLE">Single — admits 1</option>
                  <option value="DOUBLE">Double — admits 2</option>
                </NativeSelect>
              </div>
              <div className="space-y-2">
                <Label htmlFor="phone">Phone</Label>
                <Input
                  id="phone"
                  placeholder="+255 712 000 000"
                  value={formData.phone}
                  onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="jordan@example.com"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                />
              </div>
            </div>
            <SheetFooter>
              <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)}>
                Cancel
              </Button>
              <Button type="submit">Save guest</Button>
            </SheetFooter>
          </form>
        </SheetContent>
      </Sheet>

      <Dialog open={isEventModalOpen} onOpenChange={setIsEventModalOpen}>
        <DialogContent>
          <form onSubmit={handleCreateEvent}>
            <DialogHeader>
              <DialogTitle>New event</DialogTitle>
              <DialogDescription>Guests belong to an event. Name it, then add the list.</DialogDescription>
            </DialogHeader>
            <div className="grid gap-4 py-4">
              {eventFormError ? <Alert variant="destructive">{eventFormError}</Alert> : null}
              <div className="space-y-2">
                <Label htmlFor="new-event-name">Event name</Label>
                <Input
                  id="new-event-name"
                  value={newEventName}
                  onChange={(e) => setNewEventName(e.target.value)}
                  placeholder="Gold wedding"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="new-event-date">Date</Label>
                <Input
                  id="new-event-date"
                  type="date"
                  value={newEventDate}
                  onChange={(e) => setNewEventDate(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="new-event-time">Time</Label>
                <Input
                  id="new-event-time"
                  type="time"
                  value={newEventTime}
                  onChange={(e) => setNewEventTime(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="new-event-venue">Venue</Label>
                <Input
                  id="new-event-venue"
                  value={newEventVenue}
                  onChange={(e) => setNewEventVenue(e.target.value)}
                  placeholder="The Slipway, Dar es Salaam"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="new-event-type">Occasion</Label>
                <NativeSelect
                  id="new-event-type"
                  value={newEventType}
                  onChange={(e) => setNewEventType(e.target.value)}
                >
                  {EVENT_TYPES.map((type) => (
                    <option key={type.id} value={type.id}>
                      {type.label}
                    </option>
                  ))}
                </NativeSelect>
              </div>
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setIsEventModalOpen(false)}>
                Cancel
              </Button>
              <Button type="submit" loading={savingEvent}>
                Create event
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={!!deletingGuestId} onOpenChange={(open) => !open && setDeletingGuestId(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete this guest?</DialogTitle>
            <DialogDescription>This cannot be undone.</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeletingGuestId(null)}>
              Keep guest
            </Button>
            <Button variant="destructive" onClick={handleDeleteGuest}>
              Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Sheet open={isImportModalOpen} onOpenChange={setIsImportModalOpen}>
        <SheetContent className="max-w-3xl">
          <SheetHeader>
            <SheetTitle>Import guests</SheetTitle>
            <SheetDescription>
              Step {importStep} of 3 — upload a list, review rows, then confirm.
            </SheetDescription>
          </SheetHeader>
          <div className="flex-1 overflow-y-auto px-6">

          {importStep === 1 ? (
            <div className="space-y-6">
              <div className="space-y-2">
                <Label htmlFor="import-channel">Delivery channel</Label>
                <NativeSelect
                  id="import-channel"
                  value={deliveryChannel}
                  onChange={(e) => setDeliveryChannel(e.target.value)}
                >
                  <option value="BOTH">Email and SMS / WhatsApp</option>
                  <option value="WHATSAPP">SMS / WhatsApp only</option>
                  <option value="EMAIL">Email only</option>
                </NativeSelect>
              </div>
              <label
                htmlFor="file-upload"
                className="flex min-h-32 cursor-pointer flex-col items-center justify-center rounded-md border border-dashed border-input bg-muted/40 px-6 py-8 text-center"
              >
                <input
                  id="file-upload"
                  type="file"
                  accept=".csv,.xlsx,.xls"
                  className="sr-only"
                  onChange={(e) => {
                    if (e.target.files?.[0]) setSelectedFile(e.target.files[0]);
                  }}
                />
                <p className="text-sm font-medium">
                  {selectedFile ? selectedFile.name : "Choose a CSV or Excel file"}
                </p>
                <p className="mt-1 text-xs text-muted-foreground">.csv, .xlsx, .xls — up to 10MB</p>
              </label>
              <SheetFooter>
                <Button type="button" variant="outline" onClick={() => setIsImportModalOpen(false)}>
                  Cancel
                </Button>
                <Button disabled={!selectedFile || isUploading} onClick={handleUploadAndPreview}>
                  {isUploading ? "Parsing…" : "Preview rows"}
                </Button>
              </SheetFooter>
            </div>
          ) : null}

          {importStep === 2 && previewData ? (
            <div className="space-y-4">
              <div className="flex flex-wrap gap-2">
                <Button variant={previewFilter === "ALL" ? "secondary" : "outline"} onClick={() => setPreviewFilter("ALL")}>
                  All ({previewData.totalRows})
                </Button>
                <Button
                  variant={previewFilter === "VALID_ONLY" ? "secondary" : "outline"}
                  onClick={() => setPreviewFilter("VALID_ONLY")}
                >
                  Valid ({previewData.validCount})
                </Button>
                <Button
                  variant={previewFilter === "ERRORS_ONLY" ? "secondary" : "outline"}
                  onClick={() => setPreviewFilter("ERRORS_ONLY")}
                >
                  Errors ({previewData.invalidCount})
                </Button>
              </div>
              <div className="grid grid-cols-3 gap-3">
                <Card className="p-4">
                  <p className="text-xs text-muted-foreground">Ready</p>
                  <p className="font-display text-2xl tabular-nums">{previewData.validCount}</p>
                </Card>
                <Card className="p-4">
                  <p className="text-xs text-muted-foreground">Errors</p>
                  <p className="font-display text-2xl tabular-nums">{previewData.invalidCount}</p>
                </Card>
                <Card className="p-4">
                  <p className="text-xs text-muted-foreground">Duplicates</p>
                  <p className="font-display text-2xl tabular-nums">{previewData.duplicateCount}</p>
                </Card>
              </div>
              <div className="max-h-72 overflow-auto rounded-md border border-border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Row</TableHead>
                      <TableHead>Name</TableHead>
                      <TableHead>Phone</TableHead>
                      <TableHead>Email</TableHead>
                      <TableHead>Status</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredPreviewRows.map((row) => (
                      <TableRow key={row.rowNumber}>
                        <TableCell className="font-mono text-xs">#{row.rowNumber}</TableCell>
                        <TableCell>{row.fullName || "Missing"}</TableCell>
                        <TableCell className="font-mono text-xs">{row.phone || "—"}</TableCell>
                        <TableCell>{row.email || "—"}</TableCell>
                        <TableCell>
                          {row.valid ? (
                            <Badge variant="success">Valid</Badge>
                          ) : (
                            row.errors.map((err) => (
                              <Badge key={err} variant="destructive" className="mr-1">
                                {err}
                              </Badge>
                            ))
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
              <SheetFooter>
                <Button variant="outline" onClick={() => setImportStep(1)}>
                  Back
                </Button>
                <Button disabled={previewData.validCount === 0 || isUploading} onClick={handleConfirmImport}>
                  {isUploading ? "Importing…" : `Import ${previewData.validCount} guests`}
                </Button>
              </SheetFooter>
            </div>
          ) : null}

          {importStep === 3 && importResult ? (
            <div className="space-y-6 py-4">
              <p className="text-sm leading-6 text-muted-foreground">
                Imported {importResult.importedCount} guests.
                {importResult.skippedCount > 0
                  ? ` ${importResult.skippedCount} invalid or duplicate rows were skipped.`
                  : null}
              </p>
              <SheetFooter>
                <Button onClick={() => setIsImportModalOpen(false)}>View directory</Button>
              </SheetFooter>
            </div>
          ) : null}
          </div>
        </SheetContent>
      </Sheet>

      {deliveryModalGuest ? (
        <DeliveryManagerModal
          isOpen={isDeliveryModalOpen}
          onClose={() => setIsDeliveryModalOpen(false)}
          invitationId={deliveryModalInvitationId}
          guestName={deliveryModalGuest.fullName}
          guestEmail={deliveryModalGuest.email}
          guestPhone={deliveryModalGuest.phone}
        />
      ) : null}
      <BatchSendDialog
        open={isBatchSendOpen}
        onOpenChange={setIsBatchSendOpen}
        eventId={selectedEventId}
        eventName={events.find((event) => event.id === selectedEventId)?.eventName ?? ""}
        guests={guests}
      />
      <PressedCardDialog invitation={viewingCard} onClose={() => setViewingCard(null)} />
    </div>
  );
}
