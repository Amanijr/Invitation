import { t } from "@/lib/i18n";

const STEPS = [
  { id: "01", href: "#samples", titleKey: "howStep1Title", bodyKey: "howStep1Body" },
  { id: "02", href: "#personalize", titleKey: "howStep2Title", bodyKey: "howStep2Body" },
  { id: "03", href: "#delivery", titleKey: "howStep3Title", bodyKey: "howStep3Body" },
] as const;

export function HowItWorks() {
  return (
    <section id="how" className="landing-section scroll-mt-28 border-t border-border md:scroll-mt-20">
      <div className="landing-container">
        <p className="text-sm text-muted-foreground">{t("howEyebrow")}</p>
        <h2 className="mt-4 font-display text-2xl font-semibold md:text-3xl">{t("howTitle")}</h2>
        <p className="mt-3 max-w-xl text-sm leading-6 text-muted-foreground">{t("howBody")}</p>
        <ol className="mt-10 grid min-w-0 gap-8 md:grid-cols-3 md:gap-6">
          {STEPS.map((step) => (
            <li key={step.id} className="min-w-0 border-t border-border pt-6">
              <p className="font-mono text-xs tabular-nums text-muted-foreground">{step.id}</p>
              <h3 className="mt-3 font-display text-xl font-semibold">
                <a href={step.href} className="press hover:underline">
                  {t(step.titleKey)}
                </a>
              </h3>
              <p className="mt-3 text-sm leading-6 text-muted-foreground">{t(step.bodyKey)}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
