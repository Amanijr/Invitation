import { Check } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CATALOG_PACKAGES, type CatalogPackage } from "@/lib/catalog";
import { t } from "@/lib/i18n";
import { cn } from "@/lib/utils";

function formatPriceFigure(amount: number): string {
  return new Intl.NumberFormat("en-TZ").format(amount);
}

export function PackagePricing({
  packageId,
  onSelect,
  onContinue,
}: {
  packageId: string;
  onSelect: (id: string) => void;
  onContinue: (pkg: CatalogPackage) => void;
}) {
  return (
    <section id="packages" className="landing-section scroll-mt-28 border-t border-border md:scroll-mt-20">
      <div className="landing-container">
        <p className="text-sm text-muted-foreground">{t("packagesEyebrow")}</p>
        <h2 className="mt-4 font-display text-2xl font-semibold md:text-3xl">{t("packagesTitle")}</h2>
        <p className="mt-3 max-w-xl text-sm leading-6 text-muted-foreground">{t("packagesBody")}</p>

        <ul className="mt-10 grid min-w-0 items-stretch gap-4 lg:grid-cols-3">
          {CATALOG_PACKAGES.map((item) => {
            const selected = packageId === item.id;
            return (
              <li key={item.id} className="min-w-0">
                <article
                  className={cn(
                    "flex h-full min-w-0 flex-col border bg-card px-5 py-6",
                    selected ? "border-accent" : "border-border"
                  )}
                >
                  <div className="flex items-start justify-between gap-3">
                    <h3 className="font-display text-xl font-semibold">{item.name}</h3>
                    {item.recommended ? (
                      <span className="shrink-0 rounded-sm bg-accent px-2 py-1 text-[0.65rem] font-medium uppercase tracking-[0.12em] text-accent-foreground">
                        {t("usualJob")}
                      </span>
                    ) : null}
                  </div>
                  <p className="mt-2 text-sm leading-6 text-muted-foreground">{item.catchphrase}</p>

                  <p className="mt-6">
                    <span className="block font-mono text-xs uppercase tracking-[0.14em] text-muted-foreground">
                      {t("priceFrom")} TZS
                    </span>
                    <span className="mt-1 block font-display text-4xl font-semibold tabular-nums leading-none">
                      {formatPriceFigure(item.priceFrom)}
                    </span>
                    <span className="mt-2 block text-sm text-muted-foreground">
                      {t("perEvent")} · {item.guests}
                    </span>
                  </p>

                  <p className="mt-4 text-xs leading-5 text-muted-foreground">{item.channels.join(" · ")}</p>

                  <ul className="mt-6 flex flex-1 flex-col gap-3">
                    {item.includes.map((line) => (
                      <li key={line} className="flex gap-2 text-sm leading-6">
                        <Check className="mt-0.5 size-4 shrink-0 text-success" aria-hidden />
                        <span>{line}</span>
                      </li>
                    ))}
                  </ul>

                  <div className="mt-8 flex flex-col gap-2">
                    <Button
                      type="button"
                      variant={selected ? "default" : "outline"}
                      className="w-full"
                      aria-pressed={selected}
                      onClick={() => {
                        onSelect(item.id);
                        if (selected) onContinue(item);
                      }}
                    >
                      {selected ? t("continue") : t("choosePackage")}
                    </Button>
                  </div>
                </article>
              </li>
            );
          })}
        </ul>

        <p className="mt-6 max-w-2xl text-xs leading-5 text-muted-foreground">{t("packagesNote")}</p>
      </div>
    </section>
  );
}
