import { motion } from "motion/react";
import { useState, type ReactNode } from "react";
import { Link, useNavigate } from "react-router-dom";
import { DeliveryStory } from "@/components/landing/DeliveryStory";
import { DeskFeatures } from "@/components/landing/DeskFeatures";
import { FeaturedOccasions } from "@/components/landing/FeaturedOccasions";
import { HowItWorks } from "@/components/landing/HowItWorks";
import { PackagePricing } from "@/components/landing/PackagePricing";
import { PersonalizationStory } from "@/components/landing/PersonalizationStory";
import { ProofStrip } from "@/components/landing/ProofStrip";
import { TemplateShowcase } from "@/components/landing/TemplateShowcase";
import { VerificationStory } from "@/components/landing/VerificationStory";
import { Button } from "@/components/ui/button";
import { CATALOG_PACKAGES, getPackage, type CatalogTemplate, type Occasion } from "@/lib/catalog";
import { findTemplate, usePressCatalog } from "@/hooks/usePressCatalog";
import { useReducedMotion } from "@/hooks/useReducedMotion";
import { t } from "@/lib/i18n";
import { formatMoney } from "@/lib/locale";
import { uiSpring } from "@/lib/physics";
import { isSignedIn, setIntent } from "@/lib/session";

function HeroCopy({
  reducedMotion,
  children,
}: {
  reducedMotion: boolean;
  children: ReactNode;
}) {
  if (reducedMotion) {
    return <div>{children}</div>;
  }
  return (
    <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={uiSpring}>
      {children}
    </motion.div>
  );
}

function LandingNav({ className }: { className?: string }) {
  return (
    <nav className={className} aria-label="Landing">
      <a href="#how" className="press inline-flex h-11 shrink-0 items-center px-2.5 text-sm font-medium md:px-3">
        {t("navHow")}
      </a>
      <a href="#samples" className="press inline-flex h-11 shrink-0 items-center px-2.5 text-sm font-medium md:px-3">
        {t("navCards")}
      </a>
      <a href="#packages" className="press inline-flex h-11 shrink-0 items-center px-2.5 text-sm font-medium md:px-3">
        {t("navPackages")}
      </a>
    </nav>
  );
}

export function LandingPage() {
  const navigate = useNavigate();
  const reducedMotion = useReducedMotion();
  const { templates } = usePressCatalog();
  const [occasion, setOccasion] = useState<"ALL" | Occasion>("ALL");
  const [templateId, setTemplateId] = useState("");
  const [packageId, setPackageId] = useState("house-list");

  const selectedTemplate = findTemplate(templates, templateId, occasion);
  const selectedPackage = getPackage(packageId) ?? CATALOG_PACKAGES[1];
  const signedIn = isSignedIn();

  const continueToDesk = (nextPackageId = packageId, nextTemplateId = selectedTemplate.id) => {
    setIntent({
      templateId: nextTemplateId,
      packageId: nextPackageId,
      templateName: selectedTemplate.name,
      occasion: selectedTemplate.occasion,
    });
    if (signedIn) {
      navigate("/templates");
      return;
    }
    navigate("/auth?mode=register&next=/templates");
  };

  const selectFeatured = (template: CatalogTemplate) => {
    setTemplateId(template.id);
    setOccasion(template.occasion);
  };

  return (
    <div className="landing min-h-dvh bg-background pb-[calc(9rem+env(safe-area-inset-bottom))] text-foreground">
      <a href="#main" className="skip-link">
        {t("skipToContent")}
      </a>
      <header className="chrome sticky top-0 z-20">
        <div className="flex h-14 items-center justify-between gap-3 px-4 md:h-16 md:px-8">
          <Link to="/" className="press shrink-0 font-display text-lg font-semibold">
            InviteFlow
          </Link>
          <LandingNav className="hidden min-w-0 flex-1 items-center justify-end gap-1 overflow-x-auto px-4 md:flex" />
          <div className="shrink-0">
            {signedIn ? (
              <Button variant="outline" asChild>
                <Link to="/templates">{t("desk")}</Link>
              </Button>
            ) : (
              <Button variant="outline" asChild>
                <Link to="/auth">{t("signIn")}</Link>
              </Button>
            )}
          </div>
        </div>
        <LandingNav className="flex gap-1 overflow-x-auto border-t border-border px-4 md:hidden" />
      </header>

      <main id="main" className="min-w-0">
        <section className="pt-10 pb-[var(--landing-section)] md:pt-16">
          <div className="landing-container min-w-0">
            <HeroCopy reducedMotion={reducedMotion}>
              <div className="mx-auto max-w-2xl text-center">
                <p className="text-sm text-muted-foreground">{t("heroEyebrow")}</p>
                <div className="mx-auto mt-3 h-px w-10 bg-accent" aria-hidden />
                <h1 className="landing-display mt-6 font-display font-semibold">
                  {t("heroTitle")}
                  <br />
                  {t("heroTitleEnd")}
                </h1>
                <p className="mx-auto mt-6 max-w-md text-base leading-7 text-muted-foreground">{t("heroBody")}</p>
                <div className="mt-8 flex flex-col items-stretch justify-center gap-2 sm:flex-row sm:items-center">
                  <Button className="w-full sm:w-auto" onClick={() => continueToDesk()}>
                    {t("continue")}
                  </Button>
                  <Button variant="outline" className="w-full sm:w-auto" asChild>
                    <a href="#packages">{t("seePricing")}</a>
                  </Button>
                </div>
              </div>
            </HeroCopy>
            <FeaturedOccasions
              selectedId={selectedTemplate.id}
              catalog={templates}
              reducedMotion={reducedMotion}
              onSelect={selectFeatured}
            />
          </div>
        </section>

        <ProofStrip />
        <HowItWorks />
        <PersonalizationStory template={selectedTemplate} reducedMotion={reducedMotion} />
        <TemplateShowcase
          occasion={occasion}
          templateId={selectedTemplate.id}
          templates={templates}
          reducedMotion={reducedMotion}
          onOccasionChange={setOccasion}
          onSelect={setTemplateId}
        />
        <DeliveryStory template={selectedTemplate} selectedPackage={selectedPackage} reducedMotion={reducedMotion} />
        <VerificationStory template={selectedTemplate} reducedMotion={reducedMotion} />
        <DeskFeatures />
        <PackagePricing
          packageId={packageId}
          onSelect={setPackageId}
          onContinue={(pkg) => continueToDesk(pkg.id)}
        />

        <section className="landing-section border-t border-border">
          <div className="landing-container text-center">
            <h2 className="font-display text-2xl font-semibold md:text-3xl">{t("closeTitle")}</h2>
            <p className="mx-auto mt-3 max-w-md text-sm leading-6 text-muted-foreground">{t("closeBody")}</p>
            <div className="mt-8 flex flex-col items-stretch justify-center gap-2 sm:flex-row sm:items-center">
              <Button className="w-full sm:w-auto" onClick={() => continueToDesk()}>
                {t("continue")}
              </Button>
              <Button variant="outline" className="w-full sm:w-auto" asChild>
                <a href="#how">{t("seeHow")}</a>
              </Button>
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t border-border py-10">
        <div className="landing-container flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <p className="font-display text-sm font-semibold">{t("footerMark")}</p>
          <p className="text-xs text-muted-foreground">{t("footerLine")}</p>
        </div>
      </footer>

      <div
        className="chrome fixed inset-x-0 bottom-0 z-20 border-t border-border px-4 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] md:px-8"
        role="region"
        aria-label={t("selectedJob")}
      >
        <div className="mx-auto flex max-w-6xl flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-sm leading-6" aria-live="polite">
            <span className="font-medium">{selectedTemplate.name}</span>
            <span className="text-muted-foreground"> on </span>
            <span className="font-medium">{selectedPackage.name}</span>
            <span className="text-muted-foreground"> · </span>
            <span className="font-mono tabular-nums">
              {t("priceFrom")} {formatMoney(selectedPackage.priceFrom)}
            </span>
          </p>
          <Button className="w-full shrink-0 sm:w-auto" onClick={() => continueToDesk()}>
            {t("continue")}
          </Button>
        </div>
      </div>
    </div>
  );
}
