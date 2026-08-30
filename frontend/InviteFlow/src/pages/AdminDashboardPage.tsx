import { type FormEvent, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { PageHeader } from "@/components/layout/PageHeader";
import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
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
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { readError, readJson, apiFetch, eventsListPath } from "@/lib/api";
import { EVENT_TYPES } from "@/lib/catalog";
import { getActiveEventId, getUserId, setActiveEventId, syncActiveEventId } from "@/lib/session";

interface DashboardMetrics {
  eventId?: string;
  eventName?: string;
  totalEvents: number;
  totalGuests: number;
  invitationsGenerated: number;
  invitationsSent: number;
  emailDeliveries: number;
  smsDeliveries: number;
  whatsAppDeliveries: number;
  verifiedGuests: number;
  unverifiedGuests: number;
  failedDeliveries: number;
}

interface EventItem {
  id: string;
  eventName: string;
  venue?: string;
  eventDate?: string;
  eventType?: string;
  status?: string;
  currentTemplateId?: string;
  currentTemplateVersion?: number;
}

interface TemplateItem {
  id: string;
  templateName: string;
  active: boolean;
}

interface GuestItem {
  id: string;
  eventId: string;
  fullName: string;
  phone?: string;
  email?: string;
}

interface InvitationItem {
  id: string;
  guestId: string;
  used: boolean;
}

interface DeliveryLogItem {
  id: string;
  invitationId: string;
  guestId?: string;
  guestName?: string;
  channel: string;
  status: string;
  recipientContact?: string;
  errorMessage?: string;
}

type DeliveryChannelId = "EMAIL" | "SMS" | "WHATSAPP";

interface CheckInLogItem {
  id: string;
  scannerId?: string;
  token?: string;
  result: string;
  scannedAt?: string;
}

type TabId = "overview" | "events" | "guests" | "deliveries" | "audit";

function isDeliveryReceived(status?: string) {
  const value = (status ?? "").toUpperCase();
  return value === "DELIVERED" || value === "SENT";
}

function deliveryStatusLabel(status?: string) {
  const value = (status ?? "").toUpperCase();
  if (value === "DELIVERED") return "Delivered";
  if (value === "SENT") return "Sent";
  if (value === "FAILED") return "Not received";
  if (value === "PROCESSING") return "Sending";
  return status || "Unknown";
}

function deliveryStatusVariant(status?: string): "success" | "warning" | "destructive" {
  const value = (status ?? "").toUpperCase();
  if (value === "DELIVERED" || value === "SENT") return "success";
  if (value === "PROCESSING" || value === "PENDING") return "warning";
  return "destructive";
}

function MetricCard({ label, value, hint }: { label: string; value: number; hint: string }) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardDescription>{label}</CardDescription>
        <CardTitle className="font-display text-3xl tabular-nums">{value}</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-xs text-muted-foreground">{hint}</p>
      </CardContent>
    </Card>
  );
}

export function AdminDashboardPage() {
  const navigate = useNavigate();
  const [tab, setTab] = useState<TabId>("overview");
  const [selectedEventId, setSelectedEventId] = useState(() => getActiveEventId() ?? "");
  const [events, setEvents] = useState<EventItem[]>([]);
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [guests, setGuests] = useState<GuestItem[]>([]);
  const [invitations, setInvitations] = useState<InvitationItem[]>([]);
  const [deliveryLogs, setDeliveryLogs] = useState<DeliveryLogItem[]>([]);
  const [checkInLogs, setCheckInLogs] = useState<CheckInLogItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [guestSearch, setGuestSearch] = useState("");
  const [guestFilter, setGuestFilter] = useState<"ALL" | "VERIFIED" | "UNVERIFIED">("ALL");
  const [deliveryChannelFilter, setDeliveryChannelFilter] = useState<DeliveryChannelId>("EMAIL");
  const [eventDialogOpen, setEventDialogOpen] = useState(false);
  const [editingEvent, setEditingEvent] = useState<EventItem | null>(null);
  const [eventForm, setEventForm] = useState({
    eventName: "",
    venue: "",
    eventDate: "",
    eventTime: "18:00",
    eventType: "WEDDING",
    status: "ACTIVE",
    currentTemplateId: "",
    templateScope: "NEW_GUESTS_ONLY",
    confirmAll: false,
  });
  const [templates, setTemplates] = useState<TemplateItem[]>([]);
  const [retryingLogId, setRetryingLogId] = useState<string | null>(null);
  const [pendingToggle, setPendingToggle] = useState<{ id: string; status?: string } | null>(null);

  useEffect(() => {
    fetchEvents();
    fetchTemplates();
  }, []);

  useEffect(() => {
    fetchDashboardData();
  }, [selectedEventId, tab]);

  const fetchEvents = async () => {
    try {
      const res = await apiFetch(eventsListPath());
      if (res.ok) {
        const data = await readJson<EventItem[]>(res);
        setEvents(data);
        setSelectedEventId(syncActiveEventId(data.map((event) => event.id)) ?? "");
      }
    } catch {
      /* listed on next dashboard fetch */
    }
  };

  const fetchTemplates = async () => {
    try {
      const res = await apiFetch("/templates/active");
      if (res.ok) setTemplates(await readJson<TemplateItem[]>(res));
    } catch {
      /* event sheet still works without a card list */
    }
  };

  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);
    try {
      const queryParam = selectedEventId ? `?eventId=${selectedEventId}` : "";
      const metricsRes = await apiFetch(`/dashboard/metrics${queryParam}`);
      if (metricsRes.ok) setMetrics(await readJson<DashboardMetrics>(metricsRes));

      if (tab === "events") {
        const res = await apiFetch(eventsListPath());
        if (res.ok) setEvents(await readJson<EventItem[]>(res));
      } else if (tab === "guests") {
        const url = selectedEventId ? `/guests/event/${selectedEventId}` : "/guests";
        const [guestRes, invRes] = await Promise.all([apiFetch(url), apiFetch("/invitations")]);
        if (guestRes.ok) setGuests(await readJson<GuestItem[]>(guestRes));
        if (invRes.ok) setInvitations(await readJson<InvitationItem[]>(invRes));
      } else if (tab === "deliveries") {
        const guestUrl = selectedEventId ? `/guests/event/${selectedEventId}` : "/guests";
        const [guestRes, logsRes] = await Promise.all([
          apiFetch(guestUrl),
          apiFetch("/deliveries/logs"),
        ]);
        if (guestRes.ok) setGuests(await readJson<GuestItem[]>(guestRes));
        if (logsRes.ok) setDeliveryLogs(await readJson<DeliveryLogItem[]>(logsRes));
      } else if (tab === "audit") {
        const url = selectedEventId
          ? `/check-in/history?eventId=${selectedEventId}`
          : "/check-in/history";
        const res = await apiFetch(url);
        if (res.ok) setCheckInLogs(await readJson<CheckInLogItem[]>(res));
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load analytics.");
    } finally {
      setLoading(false);
    }
  };

  const maskToken = (token?: string) => {
    if (!token) return "TOK-****";
    if (token.length <= 8) return token;
    return `${token.substring(0, 4)}…${token.substring(token.length - 4)}`;
  };

  const handleRetryDelivery = async (log: DeliveryLogItem) => {
    if (retryingLogId) return;
    setRetryingLogId(log.id);
    try {
      const res = await apiFetch(`/deliveries/logs/${log.id}/retry`, { method: "POST" });
      if (res.ok) {
        toast.success("Retry started");
        fetchDashboardData();
      } else {
        setError(await readError(res));
      }
    } catch {
      setError("Could not reach the delivery service.");
    } finally {
      setRetryingLogId(null);
    }
  };

  const handleSaveEvent = async (e: FormEvent) => {
    e.preventDefault();
    if (!eventForm.eventName.trim()) return;
    if (!editingEvent && !getUserId()) {
      setError("Sign in to create an event.");
      return;
    }
    try {
      const url = editingEvent ? `/events/${editingEvent.id}` : "/events";
      const res = await apiFetch(url, {
        method: editingEvent ? "PUT" : "POST",
        body: JSON.stringify({
          eventName: eventForm.eventName.trim(),
          venue: eventForm.venue.trim() || "To be confirmed",
          eventDate: eventForm.eventDate
            ? `${eventForm.eventDate}T${eventForm.eventTime || "18:00"}:00`
            : new Date().toISOString(),
          eventType: eventForm.eventType,
          status: eventForm.status,
          currentTemplateId: eventForm.currentTemplateId || null,
        }),
      });
      if (res.ok) {
        const saved = await readJson<EventItem>(res);
        if (eventForm.currentTemplateId && (editingEvent || saved.id)) {
          const eventId = saved.id;
          const scope = editingEvent ? eventForm.templateScope : "NEW_GUESTS_ONLY";
          if (scope === "ALL_INVITATIONS" && !eventForm.confirmAll) {
            setError("Changing the template for all invitations will regenerate existing invitations. Tick the confirmation box.");
            return;
          }
          const templateRes = await apiFetch(`/events/${eventId}/template`, {
            method: "PUT",
            body: JSON.stringify({
              templateId: eventForm.currentTemplateId,
              scope,
              confirm: scope === "ALL_INVITATIONS",
            }),
          });
          if (!templateRes.ok) {
            setError(await readError(templateRes));
            return;
          }
        }
        setEventDialogOpen(false);
        toast.success(editingEvent ? "Event updated" : "Event created");
        if (!editingEvent && saved.id) {
          setSelectedEventId(saved.id);
          setActiveEventId(saved.id);
        }
        fetchEvents();
        fetchDashboardData();
      } else {
        setError(await readError(res));
      }
    } catch {
      setError("Could not save event.");
    }
  };

  const handleToggleEventStatus = async (eventId: string, currentStatus?: string) => {
    const nextStatus = currentStatus === "ACTIVE" ? "COMPLETED" : "ACTIVE";
    try {
      const res = await apiFetch(`/events/${eventId}/status?status=${nextStatus}`, { method: "PATCH" });
      if (res.ok) {
        toast.success(`Event marked ${nextStatus.toLowerCase()}`);
        fetchEvents();
        fetchDashboardData();
      }
    } catch {
      setError("Could not update event status.");
    }
  };

  const filteredGuests = guests.filter((g) => {
    const q = guestSearch.toLowerCase();
    const matchesSearch =
      !q ||
      g.fullName.toLowerCase().includes(q) ||
      (g.email && g.email.toLowerCase().includes(q)) ||
      (g.phone && g.phone.includes(guestSearch));
    if (!matchesSearch) return false;
    const used = invitations.some((inv) => inv.guestId === g.id && inv.used);
    if (guestFilter === "VERIFIED") return used;
    if (guestFilter === "UNVERIFIED") return !used;
    return true;
  });

  const guestsById = new Map(guests.map((guest) => [guest.id, guest]));
  const eventLogs = deliveryLogs.filter((log) => {
    if (!selectedEventId) return true;
    const guest = log.guestId ? guestsById.get(log.guestId) : undefined;
    return guest?.eventId === selectedEventId;
  });
  const logsByChannel = (channel: DeliveryChannelId) =>
    eventLogs.filter((log) => log.channel.toUpperCase() === channel);

  const rate =
    metrics && metrics.totalGuests > 0 ? Math.round((metrics.verifiedGuests / metrics.totalGuests) * 100) : 0;

  return (
    <div>
      <PageHeader title="Analytics" description={metrics?.eventName ?? "All events"}>
        <div className="space-y-2">
          <Label htmlFor="event-filter" className="sr-only">
            Event
          </Label>
          <NativeSelect
            id="event-filter"
            className="min-w-48"
            value={selectedEventId}
            onChange={(e) => {
              setSelectedEventId(e.target.value);
              setActiveEventId(e.target.value || null);
            }}
          >
            <option value="">All events</option>
            {events.map((ev) => (
              <option key={ev.id} value={ev.id}>
                {ev.eventName}
              </option>
            ))}
          </NativeSelect>
        </div>
      </PageHeader>

      {error ? (
        <Alert variant="destructive" className="mb-6 flex items-center justify-between gap-4">
          <span>{error}</span>
          <Button variant="outline" onClick={fetchDashboardData}>
            Retry
          </Button>
        </Alert>
      ) : null}

      <Tabs value={tab} onValueChange={(v) => setTab(v as TabId)}>
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="events">Events</TabsTrigger>
          <TabsTrigger value="guests">Guests</TabsTrigger>
          <TabsTrigger value="deliveries">Deliveries</TabsTrigger>
          <TabsTrigger value="audit">Check-in</TabsTrigger>
        </TabsList>

        {loading ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-32" />
            ))}
          </div>
        ) : null}

        {!loading && tab === "overview" && metrics ? (
          <TabsContent value="overview" className="space-y-8">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <MetricCard label="Events" value={metrics.totalEvents} hint="On the desk" />
              <MetricCard label="Guests" value={metrics.totalGuests} hint="On the list" />
              <MetricCard label="Invitations" value={metrics.invitationsGenerated} hint="Cards pressed" />
              <MetricCard label="Checked in" value={metrics.verifiedGuests} hint="At the door" />
              <MetricCard label="Not yet arrived" value={metrics.unverifiedGuests} hint="Still expected" />
              <MetricCard label="Failed deliveries" value={metrics.failedDeliveries} hint="Need retry" />
            </div>

            <Card>
              <CardHeader>
                <CardTitle>Delivery volume</CardTitle>
                <CardDescription>By channel</CardDescription>
              </CardHeader>
              <CardContent className="grid gap-4 sm:grid-cols-3">
                <div>
                  <p className="text-xs text-muted-foreground">Email</p>
                  <p className="mt-1 font-display text-2xl tabular-nums">{metrics.emailDeliveries}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">SMS</p>
                  <p className="mt-1 font-display text-2xl tabular-nums">{metrics.smsDeliveries}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">WhatsApp</p>
                  <p className="mt-1 font-display text-2xl tabular-nums">{metrics.whatsAppDeliveries}</p>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between">
                <div>
                  <CardTitle>Check-in rate</CardTitle>
                  <CardDescription>Verified against the guest list</CardDescription>
                </div>
                <span className="font-mono text-sm tabular-nums">{rate}%</span>
              </CardHeader>
              <CardContent>
                <div className="h-2 overflow-hidden rounded-sm bg-muted">
                  <div
                    className="h-full bg-accent transition-[width] duration-300 ease-out motion-reduce:transition-none"
                    style={{ width: `${rate}%` }}
                  />
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        ) : null}

        {!loading && tab === "events" ? (
          <TabsContent value="events">
            <div className="mb-4 flex justify-end">
              <Button
                onClick={() => {
                  setEditingEvent(null);
                  setEventForm({
                    eventName: "",
                    venue: "",
                    eventDate: "",
                    eventTime: "18:00",
                    eventType: "WEDDING",
                    status: "ACTIVE",
                    currentTemplateId: "",
                    templateScope: "NEW_GUESTS_ONLY",
                    confirmAll: false,
                  });
                  setEventDialogOpen(true);
                }}
              >
                New event
              </Button>
            </div>
            <Card className="overflow-hidden p-0">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Name</TableHead>
                    <TableHead>Venue</TableHead>
                    <TableHead>Type</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {events.map((ev) => (
                    <TableRow key={ev.id}>
                      <TableCell className="font-medium">{ev.eventName}</TableCell>
                      <TableCell>{ev.venue || "—"}</TableCell>
                      <TableCell>
                        <Badge variant="secondary">{ev.eventType || "EVENT"}</Badge>
                      </TableCell>
                      <TableCell>
                        <Badge variant={ev.status === "ACTIVE" ? "success" : "warning"}>
                          {ev.status || "ACTIVE"}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => setPendingToggle({ id: ev.id, status: ev.status })}
                          >
                            {ev.status === "ACTIVE" ? "Mark complete" : "Reactivate"}
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => {
                              setEditingEvent(ev);
                              setEventForm({
                                eventName: ev.eventName || "",
                                venue: ev.venue || "",
                                eventDate: ev.eventDate ? ev.eventDate.substring(0, 10) : "",
                                eventTime:
                                  ev.eventDate && ev.eventDate.length >= 16 ? ev.eventDate.substring(11, 16) : "18:00",
                                eventType: ev.eventType || "WEDDING",
                                status: ev.status || "ACTIVE",
                                currentTemplateId: ev.currentTemplateId || "",
                                templateScope: "NEW_GUESTS_ONLY",
                                confirmAll: false,
                              });
                              setEventDialogOpen(true);
                            }}
                          >
                            Edit
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Card>
          </TabsContent>
        ) : null}

        {!loading && tab === "guests" ? (
          <TabsContent value="guests" className="space-y-4">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <h2 className="font-display text-xl font-semibold">Registered guests ({filteredGuests.length})</h2>
              <div className="flex flex-wrap gap-2">
                <NativeSelect
                  value={guestFilter}
                  onChange={(e) => setGuestFilter(e.target.value as typeof guestFilter)}
                  className="w-auto"
                >
                  <option value="ALL">All statuses</option>
                  <option value="VERIFIED">Checked in</option>
                  <option value="UNVERIFIED">Not checked in</option>
                </NativeSelect>
                <Input
                  placeholder="Search name, email, or phone"
                  value={guestSearch}
                  onChange={(e) => setGuestSearch(e.target.value)}
                  className="w-64"
                />
              </div>
            </div>
            <Card className="overflow-hidden p-0">
              {filteredGuests.length === 0 ? (
                <p className="p-12 text-center text-sm text-muted-foreground">No guests match these filters.</p>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Name</TableHead>
                      <TableHead>Phone</TableHead>
                      <TableHead>Email</TableHead>
                      <TableHead className="text-right">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredGuests.map((g) => (
                      <TableRow key={g.id}>
                        <TableCell className="font-medium">{g.fullName}</TableCell>
                        <TableCell className="font-mono text-xs">{g.phone || "—"}</TableCell>
                        <TableCell>{g.email || "—"}</TableCell>
                        <TableCell className="text-right">
                          <Button variant="outline" size="sm" onClick={() => navigate("/guests")}>
                            Open directory
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </Card>
          </TabsContent>
        ) : null}

        {!loading && tab === "deliveries" ? (
          <TabsContent value="deliveries" className="space-y-4">
            <div>
              <h2 className="font-display text-xl font-semibold">Deliveries</h2>
              <p className="text-sm text-muted-foreground">
                Name and the address used for that channel. Retry only when a send did not arrive.
              </p>
            </div>
            <Tabs
              value={deliveryChannelFilter}
              onValueChange={(value) => setDeliveryChannelFilter(value as DeliveryChannelId)}
            >
              <TabsList>
                <TabsTrigger value="EMAIL">Email ({logsByChannel("EMAIL").length})</TabsTrigger>
                <TabsTrigger value="SMS">SMS ({logsByChannel("SMS").length})</TabsTrigger>
                <TabsTrigger value="WHATSAPP">WhatsApp ({logsByChannel("WHATSAPP").length})</TabsTrigger>
              </TabsList>
              {(["EMAIL", "SMS", "WHATSAPP"] as DeliveryChannelId[]).map((channel) => {
                const logs = logsByChannel(channel);
                const contactLabel = channel === "EMAIL" ? "Email" : "Phone";
                const emptyCopy =
                  channel === "EMAIL"
                    ? "No email deliveries yet."
                    : channel === "SMS"
                      ? "No SMS deliveries yet."
                      : "No WhatsApp deliveries yet.";
                return (
                  <TabsContent key={channel} value={channel}>
                    <Card className="overflow-hidden p-0">
                      {logs.length === 0 ? (
                        <p className="p-12 text-center text-sm text-muted-foreground">{emptyCopy}</p>
                      ) : (
                        <Table>
                          <TableHeader>
                            <TableRow>
                              <TableHead>Name</TableHead>
                              <TableHead>{contactLabel}</TableHead>
                              <TableHead>Status</TableHead>
                              <TableHead className="text-right">Actions</TableHead>
                            </TableRow>
                          </TableHeader>
                          <TableBody>
                            {logs.map((log) => {
                              const guest = log.guestId ? guestsById.get(log.guestId) : undefined;
                              const name = log.guestName || guest?.fullName || "—";
                              const contact =
                                channel === "EMAIL"
                                  ? log.recipientContact || guest?.email || "—"
                                  : log.recipientContact || guest?.phone || "—";
                              const received = isDeliveryReceived(log.status);
                              return (
                                <TableRow key={log.id}>
                                  <TableCell className="font-medium">{name}</TableCell>
                                  <TableCell className={channel === "EMAIL" ? undefined : "font-mono text-xs"}>
                                    {contact}
                                  </TableCell>
                                  <TableCell>
                                    <div className="space-y-1">
                                      <Badge variant={deliveryStatusVariant(log.status)}>
                                        {deliveryStatusLabel(log.status)}
                                      </Badge>
                                      {!received && log.errorMessage ? (
                                        <p className="max-w-xs truncate text-xs text-muted-foreground">
                                          {log.errorMessage}
                                        </p>
                                      ) : null}
                                    </div>
                                  </TableCell>
                                  <TableCell className="text-right">
                                    {received ? (
                                      <span className="text-xs text-muted-foreground">Received</span>
                                    ) : (
                                      <Button
                                        variant="outline"
                                        size="sm"
                                        disabled={retryingLogId === log.id}
                                        onClick={() => handleRetryDelivery(log)}
                                      >
                                        {retryingLogId === log.id ? "Retrying…" : "Retry"}
                                      </Button>
                                    )}
                                  </TableCell>
                                </TableRow>
                              );
                            })}
                          </TableBody>
                        </Table>
                      )}
                    </Card>
                  </TabsContent>
                );
              })}
            </Tabs>
          </TabsContent>
        ) : null}

        {!loading && tab === "audit" ? (
          <TabsContent value="audit" className="space-y-4">
            <h2 className="font-display text-xl font-semibold">Check-in history ({checkInLogs.length})</h2>
            <Card className="overflow-hidden p-0">
              {checkInLogs.length === 0 ? (
                <p className="p-12 text-center text-sm text-muted-foreground">No scans recorded yet.</p>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Time</TableHead>
                      <TableHead>Result</TableHead>
                      <TableHead>Token</TableHead>
                      <TableHead>Scanner</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {checkInLogs.map((log) => (
                      <TableRow key={log.id}>
                        <TableCell className="font-mono text-xs">
                          {log.scannedAt ? new Date(log.scannedAt).toLocaleString() : "Just now"}
                        </TableCell>
                        <TableCell>
                          <Badge
                            variant={
                              log.result === "SUCCESS"
                                ? "success"
                                : log.result === "ALREADY_USED"
                                  ? "warning"
                                  : "destructive"
                            }
                          >
                            {log.result}
                          </Badge>
                        </TableCell>
                        <TableCell className="font-mono text-xs">{maskToken(log.token)}</TableCell>
                        <TableCell>{log.scannerId || "Main entrance"}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </Card>
          </TabsContent>
        ) : null}
      </Tabs>

      <Sheet open={eventDialogOpen} onOpenChange={setEventDialogOpen}>
        <SheetContent>
          <form onSubmit={handleSaveEvent} className="flex h-full flex-col">
            <SheetHeader>
              <SheetTitle>{editingEvent ? "Edit event" : "New event"}</SheetTitle>
              <SheetDescription>Name the occasion and where it is held.</SheetDescription>
            </SheetHeader>
            <div className="grid flex-1 gap-4 overflow-y-auto px-6 py-4">
              <div className="space-y-2">
                <Label htmlFor="event-name">Name</Label>
                <Input
                  id="event-name"
                  required
                  placeholder="Annual leadership summit"
                  value={eventForm.eventName}
                  onChange={(e) => setEventForm({ ...eventForm, eventName: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="event-venue">Venue</Label>
                <Input
                  id="event-venue"
                  placeholder="Convention center"
                  value={eventForm.venue}
                  onChange={(e) => setEventForm({ ...eventForm, venue: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="event-date">Date</Label>
                <Input
                  id="event-date"
                  type="date"
                  value={eventForm.eventDate}
                  onChange={(e) => setEventForm({ ...eventForm, eventDate: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="event-time">Time</Label>
                <Input
                  id="event-time"
                  type="time"
                  value={eventForm.eventTime}
                  onChange={(e) => setEventForm({ ...eventForm, eventTime: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="event-type">Type</Label>
                <NativeSelect
                  id="event-type"
                  value={eventForm.eventType}
                  onChange={(e) => setEventForm({ ...eventForm, eventType: e.target.value })}
                >
                  {EVENT_TYPES.map((type) => (
                    <option key={type.id} value={type.id}>
                      {type.label}
                    </option>
                  ))}
                </NativeSelect>
              </div>
              <div className="space-y-2">
                <Label htmlFor="event-template">Invitation card</Label>
                <NativeSelect
                  id="event-template"
                  value={eventForm.currentTemplateId}
                  onChange={(e) => setEventForm({ ...eventForm, currentTemplateId: e.target.value })}
                >
                  <option value="">Assign later</option>
                  {templates.map((template) => (
                    <option key={template.id} value={template.id}>
                      {template.templateName}
                    </option>
                  ))}
                </NativeSelect>
              </div>
              {editingEvent && eventForm.currentTemplateId ? (
                <div className="space-y-2">
                  <Label htmlFor="template-scope">When this card changes</Label>
                  <NativeSelect
                    id="template-scope"
                    value={eventForm.templateScope}
                    onChange={(e) => setEventForm({ ...eventForm, templateScope: e.target.value, confirmAll: false })}
                  >
                    <option value="NEW_GUESTS_ONLY">New guests only — keep existing cards</option>
                    <option value="UNSENT_INVITATIONS">Unsent invitations — leave sent cards</option>
                    <option value="ALL_INVITATIONS">All invitations — regenerate existing cards</option>
                  </NativeSelect>
                  {eventForm.templateScope === "ALL_INVITATIONS" ? (
                    <label className="flex items-start gap-2 text-sm">
                      <input
                        type="checkbox"
                        className="mt-1"
                        checked={eventForm.confirmAll}
                        onChange={(e) => setEventForm({ ...eventForm, confirmAll: e.target.checked })}
                      />
                      <span>Changing the template for all invitations will regenerate existing invitations.</span>
                    </label>
                  ) : null}
                </div>
              ) : null}
            </div>
            <SheetFooter>
              <Button type="button" variant="outline" onClick={() => setEventDialogOpen(false)}>
                Cancel
              </Button>
              <Button type="submit">Save event</Button>
            </SheetFooter>
          </form>
        </SheetContent>
      </Sheet>

      <Dialog open={!!pendingToggle} onOpenChange={(open) => !open && setPendingToggle(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Change event status?</DialogTitle>
            <DialogDescription>This marks the event complete or active for the team.</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setPendingToggle(null)}>
              Keep current
            </Button>
            <Button
              onClick={() => {
                if (pendingToggle) handleToggleEventStatus(pendingToggle.id, pendingToggle.status);
                setPendingToggle(null);
              }}
            >
              Confirm
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
