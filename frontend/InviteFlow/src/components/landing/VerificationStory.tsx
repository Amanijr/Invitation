import { Check, ChevronDown, QrCode } from "lucide-react";
import { motion } from "motion/react";
import { useEffect, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { CATALOG_TEMPLATES, getTemplate, type CatalogTemplate } from "@/lib/catalog";
import { getEventTheme } from "@/lib/event-theme";
import { cn } from "@/lib/utils";

const FALLBACK = getTemplate("gold-wedding") ?? CATALOG_TEMPLATES[0];

const STEPS = [
  { id: "receive", label: "Guest receives invitation", stage: "card" },
  { id: "present", label: "Guest presents QR", stage: "frame" },
  { id: "scan", label: "Organizer scans", stage: "scan" },
  { id: "verify", label: "System verifies", stage: "verify" },
  { id: "success", label: "Guest checked in", stage: "success" },
] as const;

type StageId = (typeof STEPS)[number]["stage"];

const STAGE_MS: Record<StageId, number> = {
  card: 900,
  frame: 700,
  scan: 1100,
  verify: 600,
  success: 2000,
};

const STATUS: Record<StageId, string> = {
  card: "Named card at the door",
  frame: "QR in the scanner frame",
  scan: "Scanning",
  verify: "Verifying",
  success: "Checked in",
};

export function VerificationStory({
  template = FALLBACK,
  reducedMotion,
}: {
  template?: CatalogTemplate;
  reducedMotion: boolean;
}) {
  const [stageId, setStageId] = useState<StageId>(reducedMotion ? "success" : "card");
  const [paused, setPaused] = useState(false);
  const inView = useRef(false);
  const regionRef = useRef<HTMLElement>(null);

  useEffect(() => {
    const node = regionRef.current;
    if (!node) return;
    const observer = new IntersectionObserver(
      ([entry]) => {
        inView.current = entry.isIntersecting;
      },
      { threshold: 0.35 }
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    if (reducedMotion || paused) return;
    if (stageId === "success") return;
    const id = window.setTimeout(() => {
      if (!inView.current) return;
      setStageId((current) => {
        const index = STEPS.findIndex((item) => item.stage === current);
        if (index >= STEPS.length - 1) return current;
        return STEPS[index + 1].stage;
      });
    }, STAGE_MS[stageId]);
    return () => window.clearTimeout(id);
  }, [paused, reducedMotion, stageId]);

  const play = () => {
    setPaused(false);
    setStageId("card");
  };

  return (
    <section
      id="verify"
      ref={regionRef}
      className="landing-section scroll-mt-20 border-t border-border"
      onPointerEnter={() => setPaused(true)}
      onPointerLeave={() => {
        if (!reducedMotion) setPaused(false);
      }}
      onFocusCapture={() => setPaused(true)}
      onBlurCapture={(event) => {
        if (reducedMotion) return;
        const next = event.relatedTarget;
        if (next instanceof Node && event.currentTarget.contains(next)) return;
        setPaused(false);
      }}
    >
      <div className="landing-container">
        <p className="text-sm text-muted-foreground">Verify</p>
        <h2 className="mt-4 max-w-xl font-display text-3xl font-semibold leading-tight md:text-4xl">
          The card is also the door pass.
        </h2>
        <p className="mt-6 max-w-xl text-base leading-7 text-muted-foreground">
          Each named invitation carries a unique QR. At the door the desk scans it once. Designed for unique guest
          verification — not a claim that the code cannot be shared.
        </p>

        <div className="mt-12 grid min-w-0 items-start gap-10 lg:grid-cols-[16rem_1fr] lg:gap-16">
          <ol className="order-2 lg:order-1 lg:sticky lg:top-24">
            {STEPS.map((step, index) => {
              const current = step.stage === stageId;
              return (
                <li key={step.id}>
                  <button
                    type="button"
                    aria-pressed={current}
                    onClick={() => {
                      setPaused(true);
                      setStageId(step.stage);
                    }}
                    className={cn(
                      "press min-h-11 w-full py-1 text-left text-xs font-medium uppercase tracking-[0.16em]",
                      current ? "text-foreground" : "text-muted-foreground hover:text-foreground"
                    )}
                  >
                    {step.label}
                  </button>
                  {index < STEPS.length - 1 ? (
                    <ChevronDown className="my-2 size-4 text-muted-foreground md:my-3" aria-hidden />
                  ) : null}
                </li>
              );
            })}
          </ol>

          <div className="order-1 lg:order-2">
            <p className="sr-only" aria-live="polite">
              {STATUS[stageId]}
              {stageId === "success" ? ` · ${template.guest}` : ""}
            </p>
            <ScanStage template={template} stageId={stageId} reducedMotion={reducedMotion} />
            <p className="mt-4 text-sm leading-6 text-muted-foreground">
              <span className="font-medium text-foreground">{STATUS[stageId]}.</span>{" "}
              {stageId === "success"
                ? `A later scan returns already checked in.`
                : `John Mwita’s unique QR on the gold wedding card.`}
            </p>
            <div className="mt-6">
              <Button type="button" variant="outline" onClick={play}>
                Play scan
              </Button>
            </div>
            <Facts />
          </div>
        </div>
      </div>
    </section>
  );
}

function ScanStage({
  template,
  stageId,
  reducedMotion,
}: {
  template: CatalogTemplate;
  stageId: StageId;
  reducedMotion: boolean;
}) {
  const TONE = getEventTheme(template.tone);
  const showFrame = stageId === "frame" || stageId === "scan" || stageId === "verify" || stageId === "success";
  const showLine = stageId === "scan";
  const showSuccess = stageId === "success";
  const verifying = stageId === "verify";

  return (
    <div
      className="elevation-invite max-w-[20rem] p-6"
      style={{ background: TONE.paper, color: TONE.ink }}
    >
      <p className="text-[0.65rem] font-medium uppercase tracking-[0.16em]" style={{ color: TONE.muted }}>
        {template.occasionLabel}
      </p>
      <div className="mt-3 h-px w-10" style={{ background: TONE.rule }} />
      <p className="mt-4 font-display text-xl font-semibold leading-tight">{template.hosts}</p>
      <p className="mt-2 text-sm leading-6">{template.guest}</p>
      <p className="mt-2 text-xs leading-5" style={{ color: TONE.muted }}>
        {template.venue}
      </p>

      <div className="relative mx-auto mt-8 size-40">
        {showFrame ? (
          <motion.div
            className="pointer-events-none absolute -inset-2"
            initial={reducedMotion ? false : { opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: reducedMotion ? 0 : 0.24, ease: "easeOut" }}
            aria-hidden
          >
            <span className="absolute left-0 top-0 h-6 w-6 border-l-2 border-t-2 border-primary" />
            <span className="absolute right-0 top-0 h-6 w-6 border-r-2 border-t-2 border-primary" />
            <span className="absolute bottom-0 left-0 h-6 w-6 border-b-2 border-l-2 border-primary" />
            <span className="absolute bottom-0 right-0 h-6 w-6 border-b-2 border-r-2 border-primary" />
          </motion.div>
        ) : null}

        <div
          className="relative flex size-full items-center justify-center overflow-hidden"
          style={{ border: `1px solid ${TONE.rule}` }}
        >
          <QrCode className="size-24" style={{ color: TONE.ink }} aria-hidden />
          {showLine && !reducedMotion ? (
            <motion.span
              className="absolute inset-x-0 h-px bg-primary"
              initial={{ top: "8%" }}
              animate={{ top: "92%" }}
              transition={{ duration: 0.9, ease: "linear" }}
              aria-hidden
            />
          ) : null}
          {showSuccess ? (
            <motion.div
              className="absolute inset-0 flex items-center justify-center bg-background/80"
              initial={reducedMotion ? false : { opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ duration: reducedMotion ? 0 : 0.24, ease: "easeOut" }}
            >
              <Check className="size-10 text-success" strokeWidth={1.75} aria-hidden />
            </motion.div>
          ) : null}
        </div>
      </div>

      <p className="mt-5 min-h-6 font-mono text-[0.65rem] uppercase tracking-[0.14em]" style={{ color: TONE.muted }}>
        {verifying ? "Verifying" : showSuccess ? "Checked in · John Mwita" : "Unique guest QR"}
      </p>
    </div>
  );
}

function Facts() {
  return (
    <ul className="mt-10 max-w-md space-y-6 text-sm leading-6 text-muted-foreground">
      <li>
        <p className="font-medium text-foreground">Unique on the card</p>
        <p className="mt-1">Each guest prints with their own QR. The door reads that token, not a shared house code.</p>
      </li>
      <li>
        <p className="font-medium text-foreground">One successful check-in</p>
        <p className="mt-1">The first scan checks them in. A later scan returns already checked in.</p>
      </li>
      <li>
        <p className="font-medium text-foreground">A preview of the door</p>
        <p className="mt-1">This sequence is a sample of the desk scanner. It does not verify a live invitation.</p>
      </li>
    </ul>
  );
}
