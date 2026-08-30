import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Alert } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { apiFetch, readError, readJson } from "@/lib/api";

export type BatchSendChannel = "EMAIL" | "SMS" | "WHATSAPP";

export interface BatchSendGuest {
  id: string;
  fullName: string;
  phone?: string;
  email?: string;
}

interface InvitationRecord {
  id: string;
  guestId?: string;
  guestName?: string;
}

interface BatchSendResult {
  totalInvitations: number;
  successCount: number;
  failedCount: number;
  invitationResults?: { guestName?: string; overallStatus?: string }[];
}

const CHANNELS: { id: BatchSendChannel; label: string }[] = [
  { id: "WHATSAPP", label: "WhatsApp" },
  { id: "SMS", label: "SMS" },
  { id: "EMAIL", label: "Email" },
];

function hasPhone(guest: BatchSendGuest) {
  return Boolean(guest.phone?.trim());
}

function hasEmail(guest: BatchSendGuest) {
  return Boolean(guest.email?.trim());
}

function guestMatchesChannels(guest: BatchSendGuest, channels: BatchSendChannel[]) {
  const wantsEmail = channels.includes("EMAIL");
  const wantsPhone = channels.includes("SMS") || channels.includes("WHATSAPP");
  return (wantsEmail && hasEmail(guest)) || (wantsPhone && hasPhone(guest));
}

export function planBatchSend(
  guests: BatchSendGuest[],
  invitations: InvitationRecord[],
  channels: BatchSendChannel[]
) {
  const invitationByGuest = new Map<string, InvitationRecord>();
  for (const invitation of invitations) {
    if (invitation.guestId && invitation.id) invitationByGuest.set(invitation.guestId, invitation);
  }

  const invitationIds: string[] = [];
  const noCard: string[] = [];
  const noContact: string[] = [];

  for (const guest of guests) {
    const invitation = invitationByGuest.get(guest.id);
    if (!invitation) {
      noCard.push(guest.fullName || "Guest");
      continue;
    }
    if (!guestMatchesChannels(guest, channels)) {
      noContact.push(guest.fullName || "Guest");
      continue;
    }
    invitationIds.push(invitation.id);
  }

  return { invitationIds, noCard, noContact };
}

interface BatchSendDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  eventId: string;
  eventName: string;
  guests: BatchSendGuest[];
}

export function BatchSendDialog({ open, onOpenChange, eventId, eventName, guests }: BatchSendDialogProps) {
  const [channels, setChannels] = useState<Record<BatchSendChannel, boolean>>({
    WHATSAPP: true,
    SMS: true,
    EMAIL: true,
  });
  const [invitations, setInvitations] = useState<InvitationRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [sendError, setSendError] = useState("");
  const [result, setResult] = useState<BatchSendResult | null>(null);

  useEffect(() => {
    if (!open) return;
    setResult(null);
    setSendError("");
    setLoadError("");
    setChannels({ WHATSAPP: true, SMS: true, EMAIL: true });
    if (!eventId) {
      setInvitations([]);
      return;
    }
    let cancelled = false;
    setLoading(true);
    void (async () => {
      try {
        const res = await apiFetch(`/invitations/event/${eventId}`);
        if (!res.ok) {
          if (!cancelled) setLoadError(await readError(res));
          return;
        }
        const data = await readJson<InvitationRecord[]>(res);
        if (!cancelled) setInvitations(data.filter((invitation) => invitation.id));
      } catch (error) {
        if (!cancelled) setLoadError(error instanceof Error ? error.message : "Could not load pressed cards.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [open, eventId]);

  const selectedChannels = useMemo(
    () => CHANNELS.map((channel) => channel.id).filter((id) => channels[id]),
    [channels]
  );

  const plan = useMemo(
    () => planBatchSend(guests, invitations, selectedChannels),
    [guests, invitations, selectedChannels]
  );

  const handleSend = async () => {
    setSendError("");
    if (selectedChannels.length === 0) {
      setSendError("Select at least one channel.");
      return;
    }
    if (plan.invitationIds.length === 0) {
      setSendError(
        plan.noCard.length === guests.length
          ? "Generate the cards first, then send them."
          : "No guest has both a pressed card and a matching phone or email."
      );
      return;
    }
    setSending(true);
    try {
      const res = await apiFetch("/deliveries/batch-send", {
        method: "POST",
        body: JSON.stringify({
          invitationIds: plan.invitationIds,
          channels: selectedChannels,
          idempotencyPrefix: `UI-BATCH-${eventId}-${Date.now()}`,
        }),
      });
      if (res.ok) {
        setResult(await readJson<BatchSendResult>(res));
      } else {
        setSendError(await readError(res));
      }
    } catch (error) {
      setSendError(error instanceof Error ? error.message : "Could not send.");
    } finally {
      setSending(false);
    }
  };

  const failedNames =
    result?.invitationResults
      ?.filter((item) => item.overallStatus === "FAILED")
      .map((item) => item.guestName)
      .filter((name): name is string => Boolean(name)) ?? [];

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Send all cards</DialogTitle>
          <DialogDescription>
            {eventName
              ? `Dispatch every pressed card for ${eventName} on the channels you pick.`
              : "Dispatch every pressed card on the channels you pick."}
          </DialogDescription>
        </DialogHeader>

        {result ? (
          <div className="space-y-4">
            <div className="grid grid-cols-3 gap-4">
              <div>
                <p className="text-xs text-muted-foreground">Attempted</p>
                <p className="font-display text-2xl tabular-nums">{result.totalInvitations}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">Sent</p>
                <p className="font-display text-2xl tabular-nums">{result.successCount}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">Failed</p>
                <p className="font-display text-2xl tabular-nums">{result.failedCount}</p>
              </div>
            </div>
            {failedNames.length > 0 ? (
              <Alert variant="destructive">Failed for {failedNames.join(", ")}. Retry from Analytics → Deliveries.</Alert>
            ) : (
              <p className="text-sm leading-6 text-muted-foreground">
                Full log is on{" "}
                <Link to="/admin/dashboard" className="underline underline-offset-2">
                  Analytics
                </Link>
                , under Deliveries.
              </p>
            )}
          </div>
        ) : (
          <div className="space-y-4">
            {loadError ? <Alert variant="destructive">{loadError}</Alert> : null}
            {sendError ? <Alert variant="destructive">{sendError}</Alert> : null}

            <div className="space-y-3">
              <p className="text-sm font-medium">Channels</p>
              <div className="flex flex-wrap gap-4">
                {CHANNELS.map((channel) => (
                  <label key={channel.id} className="flex min-h-11 cursor-pointer items-center gap-2 text-sm">
                    <Checkbox
                      checked={channels[channel.id]}
                      onCheckedChange={() =>
                        setChannels((current) => ({ ...current, [channel.id]: !current[channel.id] }))
                      }
                    />
                    {channel.label}
                  </label>
                ))}
              </div>
            </div>

            {loading ? (
              <p className="text-sm text-muted-foreground">Checking pressed cards…</p>
            ) : (
              <div className="space-y-2 text-sm leading-6 text-muted-foreground">
                <p>
                  {plan.invitationIds.length} of {guests.length}{" "}
                  {guests.length === 1 ? "guest" : "guests"} will be sent.
                </p>
                {plan.noCard.length > 0 ? (
                  <p>
                    {plan.noCard.length} {plan.noCard.length === 1 ? "guest has" : "guests have"} no card yet — generate
                    them first. {plan.noCard.slice(0, 3).join(", ")}
                    {plan.noCard.length > 3 ? ` and ${plan.noCard.length - 3} more` : ""}.
                  </p>
                ) : null}
                {plan.noContact.length > 0 ? (
                  <p>
                    {plan.noContact.length}{" "}
                    {plan.noContact.length === 1 ? "guest has" : "guests have"} no phone or email for the selected
                    channels.
                  </p>
                ) : null}
              </div>
            )}
          </div>
        )}

        <DialogFooter>
          {result ? (
            <Button onClick={() => onOpenChange(false)}>Done</Button>
          ) : (
            <>
              <Button variant="outline" onClick={() => onOpenChange(false)}>
                Cancel
              </Button>
              <Button disabled={sending || loading || !eventId} onClick={() => void handleSend()}>
                {sending
                  ? "Sending…"
                  : `Send ${plan.invitationIds.length || 0} ${plan.invitationIds.length === 1 ? "card" : "cards"}`}
              </Button>
            </>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
