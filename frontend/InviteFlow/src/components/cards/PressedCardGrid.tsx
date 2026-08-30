import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";

export interface PressedInvitation {
  id: string;
  guestName?: string;
}

function useInvitationCardSrc(invitationId: string | null) {
  const [src, setSrc] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    if (!invitationId) {
      setSrc(null);
      setFailed(false);
      return;
    }
    let objectUrl = "";
    let cancelled = false;
    setSrc(null);
    setFailed(false);
    void (async () => {
      try {
        const res = await apiFetch(`/invitations/${invitationId}/card`);
        if (!res.ok) {
          if (!cancelled) setFailed(true);
          return;
        }
        const blob = await res.blob();
        objectUrl = URL.createObjectURL(blob);
        if (!cancelled) setSrc(objectUrl);
      } catch {
        if (!cancelled) setFailed(true);
      }
    })();
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [invitationId]);

  return { src, failed };
}

function CardFace({ invitationId, guestName, onOpen }: { invitationId: string; guestName: string; onOpen: () => void }) {
  const { src, failed } = useInvitationCardSrc(invitationId);
  return (
    <button
      type="button"
      onClick={onOpen}
      className="press w-full overflow-hidden rounded-md border border-border bg-card text-left"
    >
      <div className="flex aspect-[3/4] items-center justify-center bg-muted">
        {src ? (
          <img src={src} alt={`Card for ${guestName}`} className="h-full w-full object-contain" />
        ) : (
          <p className="px-3 text-center text-xs text-muted-foreground">
            {failed ? "Could not load this card." : "Loading card…"}
          </p>
        )}
      </div>
      <p className="truncate px-3 py-2 text-sm font-medium">{guestName}</p>
    </button>
  );
}

export function PressedCardDialog({
  invitation,
  onClose,
}: {
  invitation: PressedInvitation | null;
  onClose: () => void;
}) {
  const { src, failed } = useInvitationCardSrc(invitation?.id ?? null);
  const guestName = invitation?.guestName?.trim() || "Guest";

  return (
    <Dialog open={!!invitation} onOpenChange={(next) => !next && onClose()}>
      <DialogContent className="max-w-3xl">
        <DialogHeader>
          <DialogTitle>{guestName}</DialogTitle>
          <DialogDescription>Pressed card with this guest’s name and QR.</DialogDescription>
        </DialogHeader>
        {src ? (
          <img src={src} alt={`Card for ${guestName}`} className="max-h-[75vh] w-full object-contain" />
        ) : (
          <p className="py-16 text-center text-sm text-muted-foreground">
            {failed ? "Could not load this card." : "Loading card…"}
          </p>
        )}
      </DialogContent>
    </Dialog>
  );
}

export function PressedCardGrid({ invitations }: { invitations: PressedInvitation[] }) {
  const [openId, setOpenId] = useState<string | null>(null);
  const open = invitations.find((invitation) => invitation.id === openId) ?? null;

  if (invitations.length === 0) return null;

  return (
    <>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {invitations.map((invitation) => (
          <CardFace
            key={invitation.id}
            invitationId={invitation.id}
            guestName={invitation.guestName?.trim() || "Guest"}
            onOpen={() => setOpenId(invitation.id)}
          />
        ))}
      </div>
      <PressedCardDialog invitation={open} onClose={() => setOpenId(null)} />
    </>
  );
}
