import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { PageHeader } from "@/components/layout/PageHeader";
import { PressedCardGrid, type PressedInvitation } from "@/components/cards/PressedCardGrid";
import { BatchSendDialog } from "@/components/delivery/BatchSendDialog";
import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { NativeSelect } from "@/components/ui/select";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { cn } from "@/lib/utils";
import { apiFetch, eventsListPath, readError, readJson } from "@/lib/api";
import { getTemplate, occasionToEventType, type Occasion } from "@/lib/catalog";
import { getIntent, setActiveEventId, clearIntent, syncActiveEventIdOrFirst } from "@/lib/session";

interface EventItem {
  id: string;
  eventName: string;
  eventType: string;
  currentTemplateId?: string;
}

interface TemplateItem {
  id: string;
  templateName: string;
  eventType: string;
  active: boolean;
}

interface GuestItem {
  id: string;
  fullName: string;
  phone?: string;
  email?: string;
}

interface BulkError {
  guestId: string;
  guestName: string;
  errorMessage: string;
}

interface BulkGenerationResult {
  eventId: string;
  templateId: string;
  totalGuests: number;
  successCount: number;
  failedCount: number;
  skippedCount: number;
  successfulInvitationIds: string[];
  errors: BulkError[];
  processedAt: string;
}

export function BulkGenerationPage() {
  const [events, setEvents] = useState<EventItem[]>([]);
  const [selectedEventId, setSelectedEventId] = useState("");
  const [templates, setTemplates] = useState<TemplateItem[]>([]);
  const [selectedTemplateId, setSelectedTemplateId] = useState("");
  const [guests, setGuests] = useState<GuestItem[]>([]);
  const [regenerationPolicy, setRegenerationPolicy] = useState<"SKIP_EXISTING" | "REGENERATE_EXISTING">(
    "SKIP_EXISTING"
  );
  const [expiryDays, setExpiryDays] = useState(30);
  const [isGenerating, setIsGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<BulkGenerationResult | null>(null);
  const [selectedGuestIds, setSelectedGuestIds] = useState<string[]>([]);
  const [selectMode, setSelectMode] = useState<"ALL" | "CUSTOM">("ALL");
  const [pressedCards, setPressedCards] = useState<PressedInvitation[]>([]);
  const [isBatchSendOpen, setIsBatchSendOpen] = useState(false);

  useEffect(() => {
    fetchEvents();
  }, []);

  useEffect(() => {
    if (selectedEventId) {
      fetchGuests(selectedEventId);
      fetchTemplates();
      fetchPressedCards(selectedEventId);
      setSelectedGuestIds([]);
      setSelectMode("ALL");
    }
  }, [selectedEventId]);

  const fetchEvents = async () => {
    try {
      const res = await apiFetch(eventsListPath());
      if (res.ok) {
        const data = await readJson<EventItem[]>(res);
        setEvents(data);
        setSelectedEventId(syncActiveEventIdOrFirst(data.map((event) => event.id)) ?? "");
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not load events.");
    }
  };

  const fetchGuests = async (eventId: string) => {
    try {
      const res = await apiFetch(`/guests/event/${eventId}`);
      if (res.ok) setGuests(await readJson<GuestItem[]>(res));
    } catch {
      /* empty list stays */
    }
  };

  const fetchTemplates = async () => {
    try {
      const res = await apiFetch("/templates/active");
      if (res.ok) {
        const data = await readJson<TemplateItem[]>(res);
        setTemplates(data);
        const intent = getIntent();
        const byId = intent?.templateId ? data.find((tpl) => tpl.id === intent.templateId) : undefined;
        const held = intent ? getTemplate(intent.templateId) : undefined;
        const wantedType = intent?.occasion
          ? occasionToEventType(intent.occasion as Occasion)
          : held
            ? occasionToEventType(held.occasion)
            : undefined;
        const currentEvent = events.find((event) => event.id === selectedEventId);
        const inherited = currentEvent?.currentTemplateId
          ? data.find((tpl) => tpl.id === currentEvent.currentTemplateId)
          : undefined;
        const match = byId ?? inherited ?? (wantedType ? data.find((tpl) => tpl.eventType === wantedType) : undefined);
        setSelectedTemplateId((match ?? data[0])?.id ?? "");
      }
    } catch {
      /* listed in UI */
    }
  };

  const fetchPressedCards = async (eventId: string) => {
    try {
      const res = await apiFetch(`/invitations/event/${eventId}`);
      if (!res.ok) {
        setPressedCards([]);
        return;
      }
      const data = await readJson<PressedInvitation[]>(res);
      setPressedCards(data.filter((invitation) => invitation.id));
    } catch {
      setPressedCards([]);
    }
  };

  const toggleGuestSelection = (guestId: string) => {
    setSelectedGuestIds((ids) =>
      ids.includes(guestId) ? ids.filter((id) => id !== guestId) : [...ids, guestId]
    );
  };

  const handleGenerateBulk = async () => {
    if (!selectedEventId || !selectedTemplateId) {
      setError("Select an event and a template.");
      return;
    }
    if (isGenerating) return;
    setIsGenerating(true);
    setError(null);
    setResult(null);
    const expiryDate = new Date();
    expiryDate.setDate(expiryDate.getDate() + expiryDays);
    const payload: Record<string, unknown> = {
      eventId: selectedEventId,
      templateId: selectedTemplateId,
      regenerationPolicy,
      expiryDate: expiryDate.toISOString(),
    };
    if (selectMode === "CUSTOM" && selectedGuestIds.length > 0) {
      payload.guestIds = selectedGuestIds;
    }
    try {
      const res = await apiFetch("/invitations/generate-bulk", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      if (res.ok) {
        setResult(await readJson<BulkGenerationResult>(res));
        clearIntent();
        await fetchPressedCards(selectedEventId);
      } else {
        setError(await readError(res));
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not run generation.");
    } finally {
      setIsGenerating(false);
    }
  };

  const selectedEvent = events.find((e) => e.id === selectedEventId);

  return (
    <div>
      <PageHeader title="Generate" description="Press each guest’s name and a unique QR onto the card.">
        <Button
          variant="outline"
          disabled={!selectedEventId || pressedCards.length === 0}
          onClick={() => setIsBatchSendOpen(true)}
        >
          Send all
        </Button>
        <Button asChild variant="outline">
          <Link to="/guests">Guests</Link>
        </Button>
        <Button asChild variant="outline">
          <Link to="/templates">Templates</Link>
        </Button>
        <Button asChild variant="outline">
          <Link to="/admin/scan">Scanner</Link>
        </Button>
      </PageHeader>

      {error ? (
        <Alert variant="destructive" className="mb-6 flex items-center justify-between gap-4">
          <span>{error}</span>
          <Button variant="outline" onClick={() => setError(null)}>
            Dismiss
          </Button>
        </Alert>
      ) : null}

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Run</CardTitle>
            <CardDescription>Event, template, audience, and expiry.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="bulk-event">Event</Label>
              <NativeSelect
                id="bulk-event"
                value={selectedEventId}
                onChange={(e) => {
                  setSelectedEventId(e.target.value);
                  setActiveEventId(e.target.value || null);
                }}
              >
                {events.map((ev) => (
                  <option key={ev.id} value={ev.id}>
                    {ev.eventName} ({ev.eventType})
                  </option>
                ))}
              </NativeSelect>
            </div>

            <div className="space-y-2">
              <Label htmlFor="bulk-template">Template</Label>
              {templates.length === 0 ? (
                <Alert>No active templates. Upload and activate one first.</Alert>
              ) : (
                <NativeSelect
                  id="bulk-template"
                  value={selectedTemplateId}
                  onChange={(e) => setSelectedTemplateId(e.target.value)}
                >
                  {templates.map((tpl) => (
                    <option key={tpl.id} value={tpl.id}>
                      {tpl.templateName} [{tpl.eventType}]
                      {selectedEvent?.currentTemplateId === tpl.id ? " · event default" : ""}
                    </option>
                  ))}
                </NativeSelect>
              )}
              <p className="text-xs text-muted-foreground">
                New guests already inherit this event’s current card. Generate is for reprints and guests added before a template was assigned.
              </p>
            </div>

            <div className="space-y-3">
              <Label>Audience</Label>
              <div className="flex flex-wrap gap-2">
                <Button
                  type="button"
                  variant={selectMode === "ALL" ? "secondary" : "outline"}
                  onClick={() => setSelectMode("ALL")}
                >
                  All guests ({guests.length})
                </Button>
                <Button
                  type="button"
                  variant={selectMode === "CUSTOM" ? "secondary" : "outline"}
                  onClick={() => setSelectMode("CUSTOM")}
                >
                  Selected ({selectedGuestIds.length})
                </Button>
              </div>
              {selectMode === "CUSTOM" ? (
                <ScrollArea className="h-40 rounded-md border border-border">
                  <div className="space-y-1 p-3">
                    {guests.length === 0 ? (
                      <p className="text-sm text-muted-foreground">No guests for this event.</p>
                    ) : (
                      guests.map((g) => (
                        <label key={g.id} className="flex min-h-11 cursor-pointer items-center gap-3 rounded-md px-2 hover:bg-muted">
                          <Checkbox
                            checked={selectedGuestIds.includes(g.id)}
                            onCheckedChange={() => toggleGuestSelection(g.id)}
                          />
                          <span className="flex-1 text-sm">{g.fullName}</span>
                          <span className="text-xs text-muted-foreground">{g.email || g.phone || "No contact"}</span>
                        </label>
                      ))
                    )}
                  </div>
                </ScrollArea>
              ) : null}
            </div>

            <div className="space-y-3">
              <Label>Existing invitations</Label>
              <div className="grid gap-3 sm:grid-cols-2">
                <button
                  type="button"
                  onClick={() => setRegenerationPolicy("SKIP_EXISTING")}
                  className={cn(
                    "rounded-md border p-4 text-left transition-colors duration-150",
                    regenerationPolicy === "SKIP_EXISTING" ? "border-accent bg-muted" : "border-border hover:bg-muted/60"
                  )}
                >
                  <p className="text-sm font-medium">Skip existing</p>
                  <p className="mt-1 text-xs leading-5 text-muted-foreground">
                    Keep previously generated cards and tokens.
                  </p>
                </button>
                <button
                  type="button"
                  onClick={() => setRegenerationPolicy("REGENERATE_EXISTING")}
                  className={cn(
                    "rounded-md border p-4 text-left transition-colors duration-150",
                    regenerationPolicy === "REGENERATE_EXISTING"
                      ? "border-accent bg-muted"
                      : "border-border hover:bg-muted/60"
                  )}
                >
                  <p className="text-sm font-medium">Regenerate</p>
                  <p className="mt-1 text-xs leading-5 text-muted-foreground">
                    Re-press cards against the selected template.
                  </p>
                </button>
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="expiry">Validity (days)</Label>
              <Input
                id="expiry"
                type="number"
                min={1}
                max={365}
                value={expiryDays}
                onChange={(e) => setExpiryDays(parseInt(e.target.value, 10) || 30)}
              />
            </div>

            <Button className="w-full" disabled={isGenerating || templates.length === 0} onClick={handleGenerateBulk}>
              {isGenerating ? "Generating…" : "Generate invitations"}
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Audience</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4 text-sm">
            <div className="flex justify-between border-b border-border py-2">
              <span className="text-muted-foreground">Event</span>
              <span className="font-medium">{selectedEvent?.eventName || "None"}</span>
            </div>
            <div className="flex justify-between border-b border-border py-2">
              <span className="text-muted-foreground">Guests</span>
              <span className="font-mono tabular-nums">{guests.length}</span>
            </div>
            <p className="text-xs leading-5 text-muted-foreground">
              Each guest receives a unique token printed as a QR on their card.
            </p>
          </CardContent>
        </Card>
      </div>

      {result ? (
        <Card className="mt-8">
          <CardHeader>
            <CardTitle>Run complete</CardTitle>
            <CardDescription>Processed {new Date(result.processedAt).toLocaleString()}</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              <div>
                <p className="text-xs text-muted-foreground">Targeted</p>
                <p className="font-display text-2xl tabular-nums">{result.totalGuests}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">Generated</p>
                <p className="font-display text-2xl tabular-nums">{result.successCount}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">Skipped</p>
                <p className="font-display text-2xl tabular-nums">{result.skippedCount}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">Failed</p>
                <p className="font-display text-2xl tabular-nums">{result.failedCount}</p>
              </div>
            </div>
            {result.errors?.length ? (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Guest</TableHead>
                    <TableHead>Reason</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {result.errors.map((err) => (
                    <TableRow key={err.guestId}>
                      <TableCell>
                        <div>{err.guestName}</div>
                        <div className="font-mono text-xs text-muted-foreground">{err.guestId}</div>
                      </TableCell>
                      <TableCell>
                        <Badge variant="destructive">{err.errorMessage}</Badge>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            ) : null}
          </CardContent>
        </Card>
      ) : null}

      {pressedCards.length > 0 ? (
        <div className="mt-8">
          <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
            <div>
              <h2 className="font-display text-xl font-semibold">Pressed cards</h2>
              <p className="mt-1 text-sm text-muted-foreground">
                One card per guest. Same layout, their name, their QR. Open a card to inspect it.
              </p>
            </div>
            <Button onClick={() => setIsBatchSendOpen(true)}>Send all</Button>
          </div>
          <PressedCardGrid invitations={pressedCards} />
        </div>
      ) : null}

      <BatchSendDialog
        open={isBatchSendOpen}
        onOpenChange={setIsBatchSendOpen}
        eventId={selectedEventId}
        eventName={selectedEvent?.eventName ?? ""}
        guests={guests}
      />
    </div>
  );
}
