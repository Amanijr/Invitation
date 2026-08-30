import { InvitationSample } from "@/components/landing/InvitationSample";
import { FEATURED_OCCASIONS, getTemplate, type CatalogTemplate } from "@/lib/catalog";
import { t } from "@/lib/i18n";
import { cn } from "@/lib/utils";

export function FeaturedOccasions({
  selectedId,
  catalog = [],
  reducedMotion,
  onSelect,
}: {
  selectedId: string;
  catalog?: CatalogTemplate[];
  reducedMotion: boolean;
  onSelect: (template: CatalogTemplate) => void;
}) {
  return (
    <ul className="mt-12 flex min-w-0 snap-x snap-mandatory gap-6 overflow-x-auto pb-2 md:grid md:grid-cols-3 md:gap-6 md:overflow-visible md:pb-0">
      {FEATURED_OCCASIONS.map((item) => {
        const template =
          catalog.find((card) => card.occasion === item.occasion) ?? getTemplate(item.templateId);
        if (!template) return null;
        const selected = template.id === selectedId;
        return (
          <li key={item.templateId} className="w-[min(18rem,80vw)] shrink-0 snap-start md:w-auto md:min-w-0">
            <button
              type="button"
              aria-pressed={selected}
              onClick={() => onSelect(template)}
              className={cn(
                "press flex w-full min-w-0 flex-col text-left outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
              )}
            >
              <span
                className={cn(
                  "block rounded-md transition-[box-shadow] duration-150 ease-out",
                  selected ? "ring-2 ring-ring ring-offset-2 ring-offset-background" : "hover:shadow-[var(--elevation-sm)]"
                )}
              >
                <InvitationSample
                  template={template}
                  reducedMotion={reducedMotion}
                  className="pointer-events-none"
                />
              </span>
              <span className="mt-4 text-[0.65rem] font-medium uppercase tracking-[0.16em] text-muted-foreground">
                {item.kicker}
              </span>
              <span className="mt-2 font-display text-lg font-semibold">{item.title}</span>
              <span className="mt-2 text-sm leading-6 text-muted-foreground">{item.body}</span>
              <span className="mt-4 text-sm font-medium">{t("startCard")} →</span>
            </button>
          </li>
        );
      })}
    </ul>
  );
}

