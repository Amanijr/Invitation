import { ChevronDown, Mail, MessageCircle, MessageSquare } from "lucide-react";
import { motion } from "motion/react";
import { useState } from "react";
import { InvitationSample } from "@/components/landing/InvitationSample";
import { Button } from "@/components/ui/button";
import { CATALOG_TEMPLATES, getTemplate, type CatalogPackage, type CatalogTemplate } from "@/lib/catalog";
import { formatPhone, SAMPLE_MSISDN } from "@/lib/locale";
import { cn } from "@/lib/utils";

const STEPS = [
  { id: "create", label: "Create" },
  { id: "personalize", label: "Personalize" },
  { id: "send", label: "Send" },
  { id: "receive", label: "Guest receives" },
] as const;

const CHANNELS = [
  {
    id: "whatsapp",
    label: "WhatsApp",
    icon: MessageCircle,
    packages: "Full press",
    onJob: (pkg: CatalogPackage) => pkg.id === "full-press",
    preview: "Dear John Mwita, you are invited to Amani & Neema. Your named card is on this message. Bring the QR to the door.",
  },
  {
    id: "sms",
    label: "SMS",
    icon: MessageSquare,
    packages: "House list and Full press",
    onJob: (pkg: CatalogPackage) => pkg.id === "house-list" || pkg.id === "full-press",
    preview: "Hi John Mwita, you're invited to Amani & Neema. View your invitation card on the link from the desk.",
  },
  {
    id: "email",
    label: "Email",
    icon: Mail,
    packages: "Every package",
    onJob: () => true,
    preview: "Dear John Mwita, we invite you to Amani & Neema. Your card is below. The door scans the QR.",
  },
] as const;

type ChannelId = (typeof CHANNELS)[number]["id"];

const FALLBACK = getTemplate("gold-wedding") ?? CATALOG_TEMPLATES[0];

export function DeliveryStory({
  template = FALLBACK,
  selectedPackage,
  reducedMotion,
}: {
  template?: CatalogTemplate;
  selectedPackage: CatalogPackage;
  reducedMotion: boolean;
}) {
  const [channelId, setChannelId] = useState<ChannelId>("whatsapp");
  const channel = CHANNELS.find((item) => item.id === channelId) ?? CHANNELS[0];
  const onThisJob = channel.onJob(selectedPackage);

  return (
    <section id="delivery" className="landing-section scroll-mt-20 border-t border-border">
      <div className="landing-container">
        <p className="text-sm text-muted-foreground">Send</p>
        <h2 className="mt-4 max-w-xl font-display text-3xl font-semibold leading-tight md:text-4xl">
          How the invitation reaches the guest.
        </h2>
        <p className="mt-6 max-w-xl text-base leading-7 text-muted-foreground">
          Guests open the card on WhatsApp. SMS and email go with the package. This is a preview of that path — not a
          live message.
        </p>

        <div className="mt-12 grid min-w-0 items-start gap-10 lg:grid-cols-[14rem_1fr] lg:gap-16">
          <ol className="lg:sticky lg:top-24">
            {STEPS.map((step, index) => {
              const current = step.id === "receive";
              return (
                <li key={step.id}>
                  <p
                    className={cn(
                      "text-xs font-medium uppercase tracking-[0.16em]",
                      current ? "text-foreground" : "text-muted-foreground"
                    )}
                  >
                    {step.label}
                    {current ? ` · ${channel.label}` : ""}
                  </p>
                  {index < STEPS.length - 1 ? (
                    <ChevronDown className="my-2 size-4 text-muted-foreground md:my-4" aria-hidden />
                  ) : (
                    <p className="mt-2 max-w-[14rem] text-sm leading-6 text-foreground">
                      They open a named invitation. The door scans the QR.
                    </p>
                  )}
                </li>
              );
            })}
          </ol>

          <div className="min-w-0">
            <ChannelSwitch channelId={channelId} onSelect={setChannelId} />
            <p className="mt-4 text-sm leading-6 text-muted-foreground" aria-live="polite">
              <span className="font-medium text-foreground">{channel.label}.</span>{" "}
              {onThisJob
                ? `On ${selectedPackage.name}.`
                : `On ${channel.packages}. ${selectedPackage.name} does not include this channel.`}
            </p>
            <ChannelPreview template={template} channelId={channelId} reducedMotion={reducedMotion} />
          </div>
        </div>
      </div>
    </section>
  );
}

function ChannelSwitch({
  channelId,
  onSelect,
}: {
  channelId: ChannelId;
  onSelect: (id: ChannelId) => void;
}) {
  return (
    <div role="group" aria-label="Delivery channel">
      <div className="flex min-w-0 flex-col gap-2 sm:flex-row sm:flex-wrap">
        {CHANNELS.map((item) => {
          const Icon = item.icon;
          const active = item.id === channelId;
          return (
            <Button
              key={item.id}
              type="button"
              aria-pressed={active}
              aria-controls="delivery-preview"
              variant={active ? "secondary" : "outline"}
              className="w-full shrink-0 sm:w-auto"
              onClick={() => onSelect(item.id)}
            >
              <Icon />
              {item.label}
            </Button>
          );
        })}
      </div>
    </div>
  );
}

function ChannelPreview({
  template,
  channelId,
  reducedMotion,
}: {
  template: CatalogTemplate;
  channelId: ChannelId;
  reducedMotion: boolean;
}) {
  const channel = CHANNELS.find((item) => item.id === channelId) ?? CHANNELS[0];
  const Icon = channel.icon;

  return (
    <div id="delivery-preview" className="mt-6 max-w-xl border border-border bg-card p-5 md:p-6">
      <p className="flex items-center gap-2 text-xs font-medium uppercase tracking-[0.16em] text-muted-foreground">
        <Icon className="size-3.5" />
        {channel.label}
      </p>
      <p className="mt-1 font-mono text-[0.65rem] uppercase tracking-[0.14em] text-muted-foreground">Sample preview</p>

      <motion.div
        key={channel.id}
        initial={reducedMotion ? false : { opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: reducedMotion ? 0 : 0.24, ease: "easeOut" }}
        className="mt-6"
      >
        {channel.id === "email" ? (
          <div className="space-y-4">
            <dl className="space-y-2 text-xs leading-5">
              <div className="flex gap-4">
                <dt className="w-14 shrink-0 text-muted-foreground">From</dt>
                <dd>InviteFlow</dd>
              </div>
              <div className="flex gap-4">
                <dt className="w-14 shrink-0 text-muted-foreground">To</dt>
                <dd>John Mwita</dd>
              </div>
              <div className="flex gap-4">
                <dt className="w-14 shrink-0 text-muted-foreground">Subject</dt>
                <dd className="font-medium">You're Invited — Amani & Neema</dd>
              </div>
            </dl>
            <div className="h-px bg-border" />
            <p className="text-sm leading-6">{channel.preview}</p>
            <CardThumb template={template} />
          </div>
        ) : channel.id === "sms" ? (
          <div className="space-y-5">
            <p className="font-mono text-[0.65rem] uppercase tracking-[0.14em] text-muted-foreground">
              InviteFlow · SMS · {formatPhone(SAMPLE_MSISDN)}
            </p>
            <p className="rounded-md bg-secondary px-4 py-3 text-sm leading-6">{channel.preview}</p>
            <CardThumb template={template} />
          </div>
        ) : (
          <div className="space-y-3">
            <p className="font-mono text-[0.65rem] uppercase tracking-[0.14em] text-muted-foreground">
              InviteFlow · {formatPhone(SAMPLE_MSISDN)}
            </p>
            <div className="rounded-md bg-secondary px-4 py-4">
              <p className="text-sm font-medium">You're Invited</p>
              <p className="mt-2 text-sm leading-6">{channel.preview}</p>
              <CardThumb template={template} className="mt-4" />
            </div>
          </div>
        )}
      </motion.div>
    </div>
  );
}

function CardThumb({ template, className }: { template: CatalogTemplate; className?: string }) {
  return (
    <div className={cn("w-[11rem]", className)}>
      <InvitationSample template={template} reducedMotion />
    </div>
  );
}
