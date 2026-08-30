import { InvitationSample } from "@/components/landing/InvitationSample";
import { Button } from "@/components/ui/button";
import {
  CATALOG_TEMPLATES,
  OCCASION_FILTERS,
  type CatalogTemplate,
  type Occasion,
} from "@/lib/catalog";
import { t } from "@/lib/i18n";
import { cn } from "@/lib/utils";

export function TemplateShowcase({
  occasion,
  templateId,
  templates = CATALOG_TEMPLATES,
  reducedMotion,
  onOccasionChange,
  onSelect,
}: {
  occasion: "ALL" | Occasion;
  templateId: string;
  templates?: CatalogTemplate[];
  reducedMotion: boolean;
  onOccasionChange: (occasion: "ALL" | Occasion) => void;
  onSelect: (templateId: string) => void;
}) {
  const cards = templates.filter((item) => occasion === "ALL" || item.occasion === occasion);
  const selected = cards.find((item) => item.id === templateId) ?? cards[0] ?? templates[0];
  const rest = cards.filter((item) => item.id !== selected.id);

  return (
    <section id="samples" className="landing-section scroll-mt-20 border-t border-border">
      <div className="landing-container">
        <p className="text-sm text-muted-foreground">{t("cardsEyebrow")}</p>
        <h2 className="mt-4 font-display text-2xl font-semibold md:text-3xl">{t("cardsTitle")}</h2>
        <p className="mt-3 max-w-xl text-sm leading-6 text-muted-foreground">{t("cardsBody")}</p>

        <div className="mt-8 flex min-w-0 gap-2 overflow-x-auto pb-1 md:flex-wrap md:overflow-visible" role="group" aria-label="Occasion">
          {OCCASION_FILTERS.map((filter) => {
            const active = occasion === filter.id;
            return (
              <Button
                key={filter.id}
                type="button"
                variant={active ? "secondary" : "outline"}
                aria-pressed={active}
                className="shrink-0"
                onClick={() => {
                  onOccasionChange(filter.id);
                  const next = templates.filter(
                    (item) => filter.id === "ALL" || item.occasion === filter.id
                  );
                  if (next.length && !next.some((item) => item.id === templateId)) {
                    onSelect(next[0].id);
                  }
                }}
              >
                {filter.label}
              </Button>
            );
          })}
        </div>

        <div className="mt-10 grid min-w-0 items-start gap-10 lg:grid-cols-[20rem_1fr] lg:gap-12">
          <div className="mx-auto w-full min-w-0 max-w-sm lg:mx-0">
            <p className="sr-only" aria-live="polite">
              {selected.name} selected
            </p>
            <TemplateTile
              template={selected}
              selected
              size="hero"
              reducedMotion={reducedMotion}
              onSelect={onSelect}
            />
            {selected.developmentExample ? (
              <p className="mt-4 font-mono text-[0.65rem] uppercase tracking-[0.14em] text-muted-foreground">
                Development example
              </p>
            ) : selected.license ? (
              <p className="mt-4 font-mono text-[0.65rem] uppercase tracking-[0.14em] text-muted-foreground">
                {selected.license}
              </p>
            ) : null}
            <p className="mt-2 text-sm font-medium">{selected.name}</p>
            <p className="mt-1 text-sm leading-6 text-muted-foreground">{selected.blurb}</p>
          </div>

          {rest.length > 0 ? (
            <div className="min-w-0">
              <p className="text-xs font-medium uppercase tracking-[0.16em] text-muted-foreground lg:mb-6">
                More in this set
              </p>
              <ul className="mt-4 flex min-w-0 snap-x snap-mandatory gap-4 overflow-x-auto pb-2 lg:mt-0 lg:grid lg:grid-cols-2 lg:overflow-visible lg:pb-0 xl:grid-cols-3">
                {rest.map((template) => (
                  <li key={template.id} className="w-[13.5rem] shrink-0 snap-start lg:w-auto">
                    <TemplateTile
                      template={template}
                      selected={false}
                      reducedMotion={reducedMotion}
                      onSelect={onSelect}
                    />
                    <p className="mt-3 text-sm font-medium">{template.name}</p>
                    <p className="mt-1 text-xs leading-5 text-muted-foreground">{template.occasionLabel}</p>
                  </li>
                ))}
              </ul>
            </div>
          ) : null}
        </div>
      </div>
    </section>
  );
}

function TemplateTile({
  template,
  selected,
  size = "default",
  reducedMotion,
  onSelect,
}: {
  template: CatalogTemplate;
  selected: boolean;
  size?: "default" | "hero";
  reducedMotion: boolean;
  onSelect: (id: string) => void;
}) {
  return (
    <button
      type="button"
      aria-pressed={selected}
      aria-label={`${template.name}, ${template.occasionLabel}${template.developmentExample ? ", development example" : ""}`}
      onClick={() => onSelect(template.id)}
      className={cn(
        "press w-full rounded-md text-left outline-none transition-[box-shadow] duration-150 ease-out hover:shadow-[var(--elevation-sm)] focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background",
        selected ? "ring-2 ring-ring ring-offset-2 ring-offset-background" : ""
      )}
    >
      <InvitationSample template={template} size={size} reducedMotion={reducedMotion} className="pointer-events-none" />
    </button>
  );
}
