import { type FormEvent, useEffect, useRef, useState } from "react";
import { Html5QrcodeScanner, Html5QrcodeScanType } from "html5-qrcode";
import { CheckCircle2, CircleAlert, CircleX, Clock, ShieldAlert, Unplug, Waypoints } from "lucide-react";
import { PageHeader } from "@/components/layout/PageHeader";
import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { NativeSelect } from "@/components/ui/select";
import { apiFetch, eventsListPath, readJson } from "@/lib/api";
import { setActiveEventId, syncActiveEventIdOrFirst } from "@/lib/session";

interface EventItem {
  id: string;
  eventName: string;
}

interface VerificationResult {
  status:
    | "VALID"
    | "ALREADY_USED"
    | "INVALID_QR"
    | "UNAUTHORIZED"
    | "EVENT_MISMATCH"
    | "EXPIRED"
    | "REVOKED"
    | "NETWORK_ERROR"
    | "CAMERA_ERROR";
  message: string;
  guestName?: string;
  eventName?: string;
  token?: string;
  scannedAt?: string;
  admissionType?: string;
  admissionLimit?: number;
  usedAdmissions?: number;
  remainingAdmissions?: number;
  revoked?: boolean;
}

const RESULT_COPY: Record<
  VerificationResult["status"],
  { title: string; Icon: typeof CheckCircle2; badge: "success" | "warning" | "destructive" | "secondary" }
> = {
  VALID: { title: "Checked in", Icon: CheckCircle2, badge: "success" },
  ALREADY_USED: { title: "Already checked in", Icon: CircleAlert, badge: "warning" },
  INVALID_QR: { title: "Unrecognized token", Icon: CircleX, badge: "destructive" },
  UNAUTHORIZED: { title: "Scanner not authorized", Icon: ShieldAlert, badge: "destructive" },
  EVENT_MISMATCH: { title: "Wrong event", Icon: Waypoints, badge: "warning" },
  EXPIRED: { title: "Pass expired", Icon: Clock, badge: "warning" },
  REVOKED: { title: "Invitation revoked", Icon: ShieldAlert, badge: "destructive" },
  NETWORK_ERROR: { title: "Could not reach the server", Icon: Unplug, badge: "secondary" },
  CAMERA_ERROR: { title: "Camera unavailable", Icon: CircleAlert, badge: "warning" },
};

export function AdminScannerPage() {
  const [events, setEvents] = useState<EventItem[]>([]);
  const [selectedEventId, setSelectedEventId] = useState("");
  const [scannerId, setScannerId] = useState("");
  const [isScannerActive, setIsScannerActive] = useState(true);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [isVerifying, setIsVerifying] = useState(false);
  const [verificationResult, setVerificationResult] = useState<VerificationResult | null>(null);
  const [manualToken, setManualToken] = useState("");
  const scannerRef = useRef<Html5QrcodeScanner | null>(null);

  useEffect(() => {
    fetchEvents();
  }, []);

  useEffect(() => {
    if (isScannerActive && !verificationResult && !isVerifying) {
      startScanner();
    } else {
      stopScanner();
    }
    return () => {
      stopScanner();
    };
  }, [isScannerActive, verificationResult, isVerifying]);

  const fetchEvents = async () => {
    try {
      const res = await apiFetch(eventsListPath());
      if (res.ok) {
        const data = await readJson<EventItem[]>(res);
        setEvents(data);
        setSelectedEventId(syncActiveEventIdOrFirst(data.map((event) => event.id)) ?? "");
      }
    } catch {
      /* scanner still usable with manual token */
    }
  };

  const startScanner = () => {
    setTimeout(() => {
      const element = document.getElementById("qr-reader");
      if (!element) return;
      if (scannerRef.current) {
        try {
          scannerRef.current.clear();
        } catch {
          /* already cleared */
        }
      }
      try {
        const scanner = new Html5QrcodeScanner(
          "qr-reader",
          {
            fps: 10,
            qrbox: { width: 250, height: 250 },
            supportedScanTypes: [Html5QrcodeScanType.SCAN_TYPE_CAMERA],
          },
          false
        );
        scanner.render(
          (decodedText) => handleQrDetected(decodedText),
          () => undefined
        );
        scannerRef.current = scanner;
        setCameraError(null);
      } catch (err) {
        setCameraError(err instanceof Error ? err.message : "Could not start the camera.");
      }
    }, 100);
  };

  const stopScanner = () => {
    if (scannerRef.current) {
      try {
        scannerRef.current.clear();
      } catch {
        /* ignore */
      }
      scannerRef.current = null;
    }
  };

  const extractToken = (rawPayload: string): string => {
    const trimmed = rawPayload.trim();
    if (trimmed.includes("/scan/")) return trimmed.substring(trimmed.lastIndexOf("/scan/") + 6);
    if (trimmed.includes("/token/")) return trimmed.substring(trimmed.lastIndexOf("/token/") + 7);
    if (trimmed.includes("token=")) {
      const urlParams = new URLSearchParams(trimmed.substring(trimmed.indexOf("?")));
      return urlParams.get("token") || trimmed;
    }
    return trimmed;
  };

  const handleQrDetected = (rawPayload: string) => {
    if (isVerifying || verificationResult) return;
    stopScanner();
    verifyTokenWithBackend(extractToken(rawPayload));
  };

  const handleManualSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!manualToken.trim() || isVerifying) return;
    stopScanner();
    verifyTokenWithBackend(manualToken.trim());
  };

  const verifyTokenWithBackend = async (token: string) => {
    setIsVerifying(true);
    setVerificationResult(null);
    try {
      const payload: Record<string, string | null> = {
        token,
        eventId: selectedEventId || null,
      };
      if (scannerId.trim()) payload.scannerId = scannerId.trim();
      const res = await apiFetch("/check-in/verify", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      if (res.ok) {
        const data = await readJson<{
          result: string;
          message?: string;
          guestName?: string;
          eventName?: string;
          scannedAt?: string;
          admissionType?: string;
          admissionLimit?: number;
          usedAdmissions?: number;
          remainingAdmissions?: number;
          revoked?: boolean;
        }>(res);
        const map: Record<string, VerificationResult["status"]> = {
          SUCCESS: "VALID",
          ALREADY_USED: "ALREADY_USED",
          INVALID_TOKEN: "INVALID_QR",
          UNAUTHORIZED: "UNAUTHORIZED",
          EVENT_MISMATCH: "EVENT_MISMATCH",
          EXPIRED: "EXPIRED",
          REVOKED: "REVOKED",
        };
        setVerificationResult({
          status: map[data.result] ?? "INVALID_QR",
          message: data.message || "Verification finished",
          guestName: data.guestName,
          eventName: data.eventName,
          token,
          scannedAt: data.scannedAt,
          admissionType: data.admissionType,
          admissionLimit: data.admissionLimit,
          usedAdmissions: data.usedAdmissions,
          remainingAdmissions: data.remainingAdmissions,
          revoked: data.revoked,
        });
      } else if (res.status === 403) {
        setVerificationResult({ status: "UNAUTHORIZED", message: "Admin scanner unauthorized", token });
      } else if (res.status === 404) {
        setVerificationResult({ status: "INVALID_QR", message: "Invalid QR token", token });
      } else {
        setVerificationResult({
          status: "NETWORK_ERROR",
          message: `Server returned error (${res.status})`,
          token,
        });
      }
    } catch {
      setVerificationResult({
        status: "NETWORK_ERROR",
        message: "Unable to reach the verification server.",
        token,
      });
    } finally {
      setIsVerifying(false);
    }
  };

  const handleNextScan = () => {
    setVerificationResult(null);
    setManualToken("");
    setIsScannerActive(true);
  };

  return (
    <div>
      <PageHeader title="Scanner" description="Verify invitations at the door.">
        <div className="flex flex-wrap items-end gap-3">
          <div className="space-y-2">
            <Label htmlFor="scan-event">Event</Label>
            <NativeSelect
              id="scan-event"
              className="min-w-48"
              value={selectedEventId}
              onChange={(e) => {
                setSelectedEventId(e.target.value);
                setActiveEventId(e.target.value || null);
              }}
            >
              {events.map((ev) => (
                <option key={ev.id} value={ev.id}>
                  {ev.eventName}
                </option>
              ))}
            </NativeSelect>
          </div>
          <div className="space-y-2">
            <Label htmlFor="gate-id">Gate ID</Label>
            <Input
              id="gate-id"
              placeholder="Optional"
              value={scannerId}
              onChange={(e) => setScannerId(e.target.value)}
              className="w-40"
            />
          </div>
        </div>
      </PageHeader>

      <div className="mx-auto max-w-xl space-y-6">
        {verificationResult ? (
          <ResultCard result={verificationResult} onNext={handleNextScan} />
        ) : (
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <div>
                <CardTitle>Camera</CardTitle>
                <CardDescription>{isVerifying ? "Checking token…" : "Point at the QR on the card"}</CardDescription>
              </div>
            </CardHeader>
            <CardContent>
              {cameraError ? (
                <Alert variant="destructive" className="mb-4">
                  {cameraError}. Allow camera access, or enter the token below.
                </Alert>
              ) : null}
              <div id="qr-reader" className="min-h-64 overflow-hidden rounded-md border border-border bg-muted" />
              {isVerifying ? <p className="mt-4 text-center text-sm text-muted-foreground">Checking with the server…</p> : null}
            </CardContent>
          </Card>
        )}

        <Card>
          <CardHeader>
            <CardTitle>Manual token</CardTitle>
            <CardDescription>Use this if the camera is unavailable or the print is damaged.</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleManualSubmit} className="flex flex-col gap-3 sm:flex-row">
              <Input
                value={manualToken}
                onChange={(e) => setManualToken(e.target.value)}
                disabled={isVerifying}
                placeholder="Invitation token"
                className="font-mono"
              />
              <Button type="submit" disabled={!manualToken.trim() || isVerifying}>
                Verify
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function ResultCard({ result, onNext }: { result: VerificationResult; onNext: () => void }) {
  const meta = RESULT_COPY[result.status];
  const Icon = meta.Icon;
  return (
    <Card>
      <CardHeader className="items-center text-center">
        <Icon className="size-10 text-accent" />
        <Badge variant={meta.badge}>{meta.title}</Badge>
        <CardTitle className="font-display text-2xl">{result.guestName || meta.title}</CardTitle>
        <CardDescription>
          {result.eventName ? `${result.eventName}. ` : null}
          {result.message}
        </CardDescription>
        {result.admissionLimit != null ? (
          <p className="text-sm text-muted-foreground">
            {result.admissionType === "DOUBLE" ? "Double" : "Single"} · used {result.usedAdmissions ?? 0} of{" "}
            {result.admissionLimit}
            {result.remainingAdmissions != null ? ` · ${result.remainingAdmissions} remaining` : ""}
          </p>
        ) : null}
        {result.scannedAt ? (
          <p className="font-mono text-xs text-muted-foreground">{new Date(result.scannedAt).toLocaleTimeString()}</p>
        ) : null}
      </CardHeader>
      <CardContent className="flex justify-center">
        <Button onClick={onNext}>Scan next</Button>
      </CardContent>
    </Card>
  );
}
