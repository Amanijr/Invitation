import { useEffect, useState } from "react";
import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { apiFetch, readJson } from "@/lib/api";

export interface DeliveryLogItem {
  id: string;
  invitationId: string;
  guestId?: string;
  channel: string;
  status: string;
  recipientContact?: string;
  providerReference?: string;
  providerResponse?: string;
  errorMessage?: string;
  idempotencyKey?: string;
  retryCount: number;
  sentAt?: string;
  deliveredAt?: string;
}

interface DeliveryManagerModalProps {
  isOpen: boolean;
  onClose: () => void;
  invitationId: string;
  guestName: string;
  guestEmail?: string;
  guestPhone?: string;
}

export function DeliveryManagerModal({
  isOpen,
  onClose,
  invitationId,
  guestName,
  guestEmail = "",
  guestPhone = "",
}: DeliveryManagerModalProps) {
  const [selectedChannels, setSelectedChannels] = useState({ EMAIL: true, SMS: true, WHATSAPP: true });
  const [recipientEmail, setRecipientEmail] = useState(guestEmail ?? "");
  const [recipientPhone, setRecipientPhone] = useState(guestPhone ?? "");
  const [isSending, setIsSending] = useState(false);
  const [logs, setLogs] = useState<DeliveryLogItem[]>([]);
  const [isLoadingLogs, setIsLoadingLogs] = useState(false);
  const [feedbackMessage, setFeedbackMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  useEffect(() => {
    setRecipientEmail(guestEmail ?? "");
    setRecipientPhone(guestPhone ?? "");
    if (isOpen && invitationId) fetchDeliveryLogs();
  }, [isOpen, invitationId, guestEmail, guestPhone]);

  const fetchDeliveryLogs = async () => {
    setIsLoadingLogs(true);
    try {
      const res = await apiFetch(`/deliveries/logs/invitation/${invitationId}`);
      if (res.ok) setLogs(await readJson<DeliveryLogItem[]>(res));
    } catch {
      /* keep previous logs */
    } finally {
      setIsLoadingLogs(false);
    }
  };

  const handleChannelToggle = (channel: "EMAIL" | "SMS" | "WHATSAPP") => {
    setSelectedChannels((prev) => ({ ...prev, [channel]: !prev[channel] }));
  };

  const handleSendMultiChannel = async () => {
    setFeedbackMessage(null);
    const channelsToSend = Object.keys(selectedChannels).filter(
      (k) => selectedChannels[k as keyof typeof selectedChannels]
    );
    if (channelsToSend.length === 0) {
      setFeedbackMessage({ type: "error", text: "Select at least one channel." });
      return;
    }
    setIsSending(true);
    try {
      const res = await apiFetch("/deliveries/send", {
        method: "POST",
        body: JSON.stringify({
          invitationId,
          channels: channelsToSend,
          recipientEmail: recipientEmail.trim() || undefined,
          recipientPhone: recipientPhone.trim() || undefined,
          idempotencyKey: `UI-DISPATCH-${invitationId}-${Date.now()}`,
        }),
      });
      if (res.ok) {
        const data = await readJson<{ overallStatus: string }>(res);
        setFeedbackMessage({ type: "success", text: `Dispatch started. Status: ${data.overallStatus}` });
        fetchDeliveryLogs();
      } else {
        setFeedbackMessage({ type: "error", text: (await res.text()) || "Delivery dispatch failed." });
      }
    } catch (e) {
      setFeedbackMessage({ type: "error", text: e instanceof Error ? e.message : "Could not send." });
    } finally {
      setIsSending(false);
    }
  };

  const handleRetryLog = async (logId: string) => {
    try {
      const res = await apiFetch(`/deliveries/logs/${logId}/retry`, { method: "POST" });
      if (res.ok) {
        setFeedbackMessage({ type: "success", text: "Retry started." });
        fetchDeliveryLogs();
      } else {
        setFeedbackMessage({ type: "error", text: "Retry failed." });
      }
    } catch {
      setFeedbackMessage({ type: "error", text: "Retry failed." });
    }
  };

  return (
    <Sheet open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <SheetContent className="max-w-3xl">
        <SheetHeader>
          <SheetTitle>Send invitation</SheetTitle>
          <SheetDescription>To {guestName}</SheetDescription>
        </SheetHeader>
        <div className="flex-1 space-y-4 overflow-y-auto px-6 pb-6">

        {feedbackMessage ? (
          <Alert variant={feedbackMessage.type === "error" ? "destructive" : "default"}>
            {feedbackMessage.text}
          </Alert>
        ) : null}

        <div className="space-y-4 rounded-md border border-border p-4">
          <p className="text-sm font-medium">Channels</p>
          <div className="flex flex-wrap gap-4">
            {(["EMAIL", "SMS", "WHATSAPP"] as const).map((channel) => (
              <label key={channel} className="flex min-h-11 cursor-pointer items-center gap-2 text-sm">
                <Checkbox
                  checked={selectedChannels[channel]}
                  onCheckedChange={() => handleChannelToggle(channel)}
                />
                {channel === "EMAIL" ? "Email" : channel === "SMS" ? "SMS" : "WhatsApp"}
              </label>
            ))}
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="send-email">Email</Label>
              <Input
                id="send-email"
                type="email"
                value={recipientEmail ?? ""}
                onChange={(e) => setRecipientEmail(e.target.value)}
                placeholder="guest@example.com"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="send-phone">Phone</Label>
              <Input
                id="send-phone"
                value={recipientPhone ?? ""}
                onChange={(e) => setRecipientPhone(e.target.value)}
                placeholder="+1234567890"
              />
            </div>
          </div>
          <div className="flex justify-end">
            <Button disabled={isSending} onClick={handleSendMultiChannel}>
              {isSending ? "Sending…" : "Send"}
            </Button>
          </div>
        </div>

        <div className="flex items-center justify-between">
          <h3 className="text-sm font-medium">Delivery log</h3>
          <Button variant="outline" size="sm" onClick={fetchDeliveryLogs}>
            Refresh
          </Button>
        </div>
        {isLoadingLogs ? (
          <p className="py-8 text-center text-sm text-muted-foreground">Loading logs…</p>
        ) : logs.length === 0 ? (
          <p className="rounded-md border border-dashed border-border py-8 text-center text-sm text-muted-foreground">
            No attempts yet. Send to start a log.
          </p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Channel</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Recipient</TableHead>
                <TableHead>Reference</TableHead>
                <TableHead>Attempts</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {logs.map((log) => {
                const isFailed = log.status === "FAILED";
                const isSent = log.status === "SENT" || log.status === "DELIVERED";
                return (
                  <TableRow key={log.id}>
                    <TableCell>
                      <Badge variant="outline">{log.channel}</Badge>
                    </TableCell>
                    <TableCell>
                      <Badge variant={isSent ? "success" : isFailed ? "destructive" : "warning"}>{log.status}</Badge>
                    </TableCell>
                    <TableCell>{log.recipientContact || "—"}</TableCell>
                    <TableCell className="font-mono text-xs">{log.providerReference || "—"}</TableCell>
                    <TableCell className="tabular-nums">{log.retryCount + 1}</TableCell>
                    <TableCell className="text-right">
                      {isFailed ? (
                        <Button variant="outline" size="sm" onClick={() => handleRetryLog(log.id)}>
                          Retry
                        </Button>
                      ) : null}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}
        </div>
      </SheetContent>
    </Sheet>
  );
}
