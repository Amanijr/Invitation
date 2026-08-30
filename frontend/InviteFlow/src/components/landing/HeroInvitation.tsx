import { ChevronDown, Mail, MessageCircle, MessageSquare } from "lucide-react";
import { motion } from "motion/react";
import { useEffect, useState } from "react";
import { ComposePlate } from "@/components/landing/ComposePlate";
import { t } from "@/lib/i18n";
import { uiSpring } from "@/lib/physics";
import { cn } from "@/lib/utils";
import type { CatalogTemplate } from "@/lib/catalog";

const CHANNELS = [
  { id: "whatsapp", label: "WhatsApp", icon: MessageCircle },
  { id: "sms", label: "SMS", icon: MessageSquare },
  { id: "email", label: "Email", icon: Mail },
] as const;

/** Time to finish the staged field reveal on the sample card. */
const INTRO_MS = 900;

function Delivery() {
  return (
    <div className="mt-6 flex w-full max-w-sm flex-col items-center">
      <ChevronDown className="size-4 text-muted-foreground" aria-hidden />
      <p className="mt-3 text-center text-xs leading-5 text-muted-foreground">{t("heroDelivery")}</p>
      <ul className="mt-3 flex w-full items-center justify-center gap-4 sm:gap-6" aria-hidden>
        {CHANNELS.map((channel) => {
          const Icon = channel.icon;
          const first = channel.id === "whatsapp";
          return (
            <li key={channel.id} className="flex flex-col items-center gap-2">
              <span
                className={cn(
                  "flex size-11 items-center justify-center rounded-md border bg-card",
                  first ? "border-accent" : "border-border"
                )}
              >
                <Icon className="size-4 text-foreground" />
              </span>
              <span className={cn("text-xs", first ? "font-medium text-foreground" : "text-muted-foreground")}>
                {channel.label}
              </span>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

export function HeroInvitation({
  template,
  reducedMotion,
  onChange,
}: {
  template: CatalogTemplate;
  reducedMotion: boolean;
  onChange: (template: CatalogTemplate) => void;
}) {
  const [introDone, setIntroDone] = useState(reducedMotion);

  useEffect(() => {
    if (reducedMotion) return;
    const id = window.setTimeout(() => setIntroDone(true), INTRO_MS);
    return () => window.clearTimeout(id);
  }, [reducedMotion]);

  const plate = (
    <ComposePlate
      template={template}
      reducedMotion={reducedMotion}
      onChange={onChange}
      staged={!reducedMotion && !introDone}
      afterCard={<Delivery />}
    />
  );

  if (reducedMotion) {
    return <div className="flex flex-col items-center">{plate}</div>;
  }

  return (
    <motion.div
      className="flex flex-col items-center"
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ ...uiSpring, delay: 0.12 }}
    >
      {plate}
    </motion.div>
  );
}
