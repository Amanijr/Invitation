import { LayoutGroup, motion } from "motion/react";
import { useEffect, useRef, useState } from "react";
import { InvitationSample } from "@/components/landing/InvitationSample";
import { Button } from "@/components/ui/button";
import { CATALOG_TEMPLATES, getTemplate, type CatalogTemplate } from "@/lib/catalog";
import { uiSpring } from "@/lib/physics";
import { cn } from "@/lib/utils";

const GUESTS = ["John Mwita", "Asha Kimaro", "Baraka Mushi"] as const;

const STEPS = [
  { id: "template", label: "Template", guest: "" },
  { id: "john", label: GUESTS[0], guest: GUESTS[0] },
  { id: "asha", label: GUESTS[1], guest: GUESTS[1] },
  { id: "baraka", label: GUESTS[2], guest: GUESTS[2] },
] as const;

type StepId = (typeof STEPS)[number]["id"];

const CYCLE_MS = 2800;
const STORY_TEMPLATE = getTemplate("gold-wedding") ?? CATALOG_TEMPLATES[0];

export function PersonalizationStory({
  template = STORY_TEMPLATE,
  reducedMotion,
}: {
  template?: CatalogTemplate;
  reducedMotion: boolean;
}) {
  const [stepId, setStepId] = useState<StepId>(reducedMotion ? "john" : "template");
  const [paused, setPaused] = useState(false);
  const inView = useRef(false);
  const regionRef = useRef<HTMLElement>(null);

  const step = STEPS.find((item) => item.id === stepId) ?? STEPS[0];
  const liveTemplate = { ...template, guest: step.guest };

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
    const last = STEPS[STEPS.length - 1].id;
    if (stepId === last) return;
    const id = window.setInterval(() => {
      if (!inView.current) return;
      setStepId((current) => {
        const index = STEPS.findIndex((item) => item.id === current);
        if (index >= STEPS.length - 1) return current;
        return STEPS[index + 1].id;
      });
    }, CYCLE_MS);
    return () => window.clearInterval(id);
  }, [paused, reducedMotion, stepId]);

  return (
    <section
      id="personalize"
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
        {reducedMotion ? (
          <>
            <p className="text-sm text-muted-foreground">Personalize</p>
            <h2 className="mt-4 max-w-xl font-display text-3xl font-semibold leading-tight md:text-4xl">
              One design.
              <span className="mt-1 block">Every guest gets their own invitation.</span>
            </h2>
            <p className="mt-6 max-w-xl text-base leading-7 text-muted-foreground">
              You set the card once. The guest list writes each name onto the same stock. John Mwita, Asha Kimaro, and
              Baraka Mushi each leave with a card that is theirs — not a reprint of the same line.
            </p>
            <StaticPress template={STORY_TEMPLATE} />
          </>
        ) : (
          <>
            <div className="grid min-w-0 items-start gap-10 lg:grid-cols-[1fr_16rem] lg:gap-16">
              <div className="min-w-0">
                <p className="text-sm text-muted-foreground">Personalize</p>
                <h2 className="mt-4 max-w-xl font-display text-3xl font-semibold leading-tight md:text-4xl">
                  One design.
                  <span className="mt-1 block">Every guest gets their own invitation.</span>
                </h2>
                <p className="mt-6 max-w-xl text-base leading-7 text-muted-foreground">
                  You set the card once. The guest list writes each name onto the same stock. John Mwita, Asha Kimaro,
                  and Baraka Mushi each leave with a card that is theirs — not a reprint of the same line.
                </p>
                <div className="mt-10">
                  <GuestList stepId={stepId} onSelect={setStepId} />
                </div>
              </div>
              <div className="mx-auto w-full max-w-[15.5rem] lg:mx-0">
                <p className="sr-only" aria-live="polite">
                  {step.guest ? `Invitation for ${step.guest}` : "Template before names are set"}
                </p>
                <InvitationSample template={liveTemplate} swapGuest />
                <p className="mt-4 text-sm text-muted-foreground">
                  {STORY_TEMPLATE.name}
                  <span className="mt-1 block font-medium text-foreground">{step.label}</span>
                </p>
              </div>
            </div>
            <Facts className="mt-12 grid max-w-none gap-8 md:grid-cols-3 md:space-y-0" />
          </>
        )}
      </div>
    </section>
  );
}

function GuestList({
  stepId,
  onSelect,
}: {
  stepId: StepId;
  onSelect: (id: StepId) => void;
}) {
  return (
    <LayoutGroup>
      <div className="min-w-0">
        <p className="text-xs font-medium uppercase tracking-[0.16em] text-muted-foreground">Guest list</p>
        <div className="mt-6 flex min-w-0 gap-2 overflow-x-auto pb-1 md:hidden">
          {STEPS.map((item) => {
            const active = item.id === stepId;
            return (
              <Button
                key={item.id}
                type="button"
                variant={active ? "secondary" : "outline"}
                aria-pressed={active}
                onClick={() => onSelect(item.id)}
                className="shrink-0"
              >
                {item.label}
              </Button>
            );
          })}
        </div>
        <ol className="relative mt-6 hidden border-l border-border md:block">
          {STEPS.map((item) => {
            const active = item.id === stepId;
            return (
              <li key={item.id} className="relative">
                {active ? (
                  <motion.span
                    layoutId="personalize-marker"
                    className="absolute -left-px top-3 h-5 w-0.5 bg-primary"
                    transition={uiSpring}
                  />
                ) : null}
                <button
                  type="button"
                  aria-pressed={active}
                  onClick={() => onSelect(item.id)}
                  className={cn(
                    "press flex min-h-11 w-full items-center py-2 pl-6 text-left text-sm leading-6",
                    active ? "font-medium text-foreground" : "text-muted-foreground hover:text-foreground"
                  )}
                >
                  {item.label}
                </button>
              </li>
            );
          })}
        </ol>
      </div>
    </LayoutGroup>
  );
}

function Facts({ className }: { className?: string }) {
  return (
    <ul className={cn("max-w-md space-y-6 text-sm leading-6 text-muted-foreground", className)}>
      <li>
        <p className="font-medium text-foreground">Personal, not duplicated</p>
        <p className="mt-1">Every card keeps the event, the guest line, and a QR. Only the name on the stock changes.</p>
      </li>
      <li>
        <p className="font-medium text-foreground">The list does the press</p>
        <p className="mt-1">Bulk generate fills each card from the guest list. You do not redraw the design for each person.</p>
      </li>
      <li>
        <p className="font-medium text-foreground">One design at the door</p>
        <p className="mt-1">House list covers up to 250 guests on the same card. Full press covers up to 1,000.</p>
      </li>
    </ul>
  );
}

function StaticPress({ template }: { template: CatalogTemplate }) {
  const cards = STEPS.filter((item) => item.id !== "template");

  return (
    <div className="mt-12">
      <p className="text-xs font-medium uppercase tracking-[0.16em] text-muted-foreground">Same card, three guests</p>
      <ul className="mt-6 flex min-w-0 snap-x snap-mandatory gap-4 overflow-x-auto pb-2 md:grid md:grid-cols-3 md:overflow-visible md:pb-0">
        {cards.map((item) => (
          <li key={item.id} className="w-[min(16rem,80vw)] shrink-0 snap-start md:w-auto">
            <InvitationSample template={{ ...template, guest: item.guest }} reducedMotion />
            <p className="mt-4 text-sm font-medium">{item.label}</p>
          </li>
        ))}
      </ul>
      <Facts className="mt-10" />
    </div>
  );
}
