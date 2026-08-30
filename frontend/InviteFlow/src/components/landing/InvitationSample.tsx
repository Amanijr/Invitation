import { motion } from "motion/react";
import { QrCode } from "lucide-react";
import { getEventTheme } from "@/lib/event-theme";
import { uiSpring } from "@/lib/physics";
import { cn } from "@/lib/utils";
import type { CatalogTemplate } from "@/lib/catalog";

const NAME_PLACEHOLDER = "Name prints here";
const swapTransition = { duration: 0.24, ease: "easeOut" as const };

export function InvitationSample({
  template,
  className,
  size = "default",
  staged = false,
  reducedMotion = false,
  swapGuest = false,
}: {
  template: CatalogTemplate;
  className?: string;
  size?: "default" | "hero";
  staged?: boolean;
  reducedMotion?: boolean;
  swapGuest?: boolean;
}) {
  if (template.fileUrl) {
    return (
      <div
        className={cn(
          "elevation-invite relative aspect-[3/4] w-full min-w-0 overflow-hidden bg-muted",
          className
        )}
      >
        <img
          src={template.fileUrl}
          alt={template.name}
          className="h-full w-full object-contain"
        />
      </div>
    );
  }

  const tone = getEventTheme(template.tone);
  const display = template.hosts ?? template.guest;
  const showGuestLine = Boolean(template.hosts);
  const guestKey = template.guest || "template";
  const animateSwap = swapGuest && !reducedMotion;

  const reveal = (delay: number) => {
    if (!staged || reducedMotion) return {};
    return {
      initial: { opacity: 0, y: 8 },
      animate: { opacity: 1, y: 0 },
      transition: { ...uiSpring, delay },
    };
  };

  const guestLine = (
    <span style={{ color: template.guest ? tone.ink : tone.muted }}>
      {template.guest || NAME_PLACEHOLDER}
    </span>
  );

  const titleClass = cn(
    "mt-4 font-display font-semibold leading-tight tracking-tight",
    size === "hero" ? "text-2xl" : "text-xl"
  );
  const titleText = display || NAME_PLACEHOLDER;

  return (
    <div
      className={cn(
        "elevation-invite relative flex aspect-[3/4] w-full min-w-0 flex-col justify-between",
        size === "hero" ? "p-7" : "p-6",
        className
      )}
      style={{ background: tone.paper, color: tone.ink }}
    >
      <div>
        <p className="text-[0.65rem] font-medium uppercase tracking-[0.16em]" style={{ color: tone.muted }}>
          {template.occasionLabel}
        </p>
        <div className="mt-3 h-px w-10" style={{ background: tone.rule }} />
        {animateSwap && !showGuestLine ? (
          <motion.p
            key={guestKey}
            className={titleClass}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={swapTransition}
          >
            {titleText}
          </motion.p>
        ) : (
          <motion.p className={titleClass} {...reveal(0)}>
            {titleText}
          </motion.p>
        )}
        {showGuestLine ? (
          animateSwap ? (
            <span className="mt-3 block min-h-6 overflow-hidden text-sm leading-6">
              <motion.p
                key={guestKey}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={swapTransition}
              >
                {guestLine}
              </motion.p>
            </span>
          ) : (
            <motion.p className="mt-3 min-h-6 text-sm leading-6" {...reveal(0.12)}>
              {guestLine}
            </motion.p>
          )
        ) : null}
        {template.line ? (
          <p className="mt-2 text-xs leading-5" style={{ color: tone.muted }}>
            {template.line}
          </p>
        ) : null}
        <p className="mt-2 text-xs leading-5" style={{ color: tone.muted }}>
          {template.date}
        </p>
      </div>
      <div className="flex items-end justify-between gap-3">
        <p className="max-w-[10rem] break-words text-xs leading-5" style={{ color: tone.muted }}>
          {template.venue}
        </p>
        <motion.div
          className="flex size-11 items-center justify-center"
          style={{ border: `1px solid ${tone.rule}` }}
          aria-hidden
          {...reveal(0.24)}
        >
          <QrCode className="size-5" />
        </motion.div>
      </div>
    </div>
  );
}
