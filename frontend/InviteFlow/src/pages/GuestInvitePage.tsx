import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { apiUrl } from "@/lib/api";

export function GuestInvitePage() {
  const { token } = useParams<{ token: string }>();
  const [src, setSrc] = useState<string | null>(null);
  const [status, setStatus] = useState<"loading" | "ready" | "missing">("loading");

  useEffect(() => {
    if (!token) {
      setStatus("missing");
      return;
    }
    let objectUrl = "";
    let cancelled = false;
    setStatus("loading");
    setSrc(null);
    void (async () => {
      try {
        const res = await fetch(apiUrl(`/invitations/token/${encodeURIComponent(token)}/card`));
        if (!res.ok) {
          if (!cancelled) setStatus("missing");
          return;
        }
        const blob = await res.blob();
        objectUrl = URL.createObjectURL(blob);
        if (!cancelled) {
          setSrc(objectUrl);
          setStatus("ready");
        }
      } catch {
        if (!cancelled) setStatus("missing");
      }
    })();
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [token]);

  return (
    <div className="min-h-dvh bg-background px-4 py-8 text-foreground">
      <header className="mx-auto mb-8 flex max-w-lg items-baseline justify-between">
        <p className="font-display text-xl font-semibold">InviteFlow</p>
        <Link to="/" className="press text-sm text-muted-foreground">
          About this desk
        </Link>
      </header>
      <main className="mx-auto max-w-lg">
        {status === "loading" ? (
          <p className="text-sm text-muted-foreground">Loading your card…</p>
        ) : null}
        {status === "missing" ? (
          <p className="text-sm text-muted-foreground">This invitation could not be found.</p>
        ) : null}
        {status === "ready" && src ? (
          <>
            <figure className="overflow-hidden rounded-md border border-border bg-card shadow-sm">
              <img src={src} alt="Your invitation card" className="block h-auto w-full" />
            </figure>
            {token ? (
              <p className="mt-6 text-sm text-muted-foreground">
                Door code{" "}
                <span className="font-mono text-foreground">{token}</span>
                <span className="block pt-1">Show this card at the door. The QR is your pass.</span>
              </p>
            ) : null}
          </>
        ) : null}
      </main>
    </div>
  );
}
