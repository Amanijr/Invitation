import { Banknote, MessageCircle, QrCode, UserRound } from "lucide-react";
import { t } from "@/lib/i18n";

const PROOF = [
  { icon: UserRound, title: "proofNamed", body: "proofNamedBody" },
  { icon: MessageCircle, title: "proofWhatsApp", body: "proofWhatsAppBody" },
  { icon: QrCode, title: "proofDoor", body: "proofDoorBody" },
  { icon: Banknote, title: "proofTzs", body: "proofTzsBody" },
] as const;

export function ProofStrip() {
  return (
    <section aria-label="Product facts" className="border-y border-border">
      <ul className="landing-container grid min-w-0 gap-6 py-8 sm:grid-cols-2 lg:grid-cols-4">
        {PROOF.map((item) => {
          const Icon = item.icon;
          return (
            <li key={item.title} className="min-w-0">
              <Icon className="size-4 text-foreground" aria-hidden />
              <p className="mt-3 text-sm font-medium">{t(item.title)}</p>
              <p className="mt-1 text-sm leading-6 text-muted-foreground">{t(item.body)}</p>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
