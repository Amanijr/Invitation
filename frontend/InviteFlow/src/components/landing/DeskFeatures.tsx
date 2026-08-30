import { MessageCircle, QrCode, Rows3, UserRound } from "lucide-react";
import { t } from "@/lib/i18n";

const FEATURES = [
  { icon: UserRound, title: "featureNamedTitle", body: "featureNamedBody" },
  { icon: MessageCircle, title: "featureSendTitle", body: "featureSendBody" },
  { icon: QrCode, title: "featureDoorTitle", body: "featureDoorBody" },
  { icon: Rows3, title: "featureBulkTitle", body: "featureBulkBody" },
] as const;

export function DeskFeatures() {
  return (
    <section className="landing-section border-t border-border">
      <div className="landing-container">
        <p className="text-sm text-muted-foreground">{t("featuresEyebrow")}</p>
        <h2 className="mt-4 font-display text-2xl font-semibold md:text-3xl">{t("featuresTitle")}</h2>
        <ul className="mt-10 grid min-w-0 gap-6 sm:grid-cols-2">
          {FEATURES.map((item) => {
            const Icon = item.icon;
            return (
              <li key={item.title} className="min-w-0 border border-border bg-card px-5 py-6">
                <Icon className="size-5 text-foreground" aria-hidden />
                <h3 className="mt-4 font-display text-lg font-semibold">{t(item.title)}</h3>
                <p className="mt-2 text-sm leading-6 text-muted-foreground">{t(item.body)}</p>
              </li>
            );
          })}
        </ul>
      </div>
    </section>
  );
}
