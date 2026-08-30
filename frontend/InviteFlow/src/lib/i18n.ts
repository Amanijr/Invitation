import { MARKET, type InterfaceLanguage } from "@/lib/locale";

/**
 * Landing chrome strings. Add a Kiswahili value in `sw` only when a speaker has written it.
 * Do not machine-fill `sw` — awkward calques stay out of the product.
 */
const en = {
  navHow: "How it works",
  navPersonalize: "Personalize",
  navCards: "Cards",
  navSend: "Send",
  navVerify: "Verify",
  navPackages: "Pricing",
  signIn: "Sign in",
  desk: "Desk",
  skipToContent: "Skip to content",
  selectedJob: "Selected card and package",
  heroEyebrow: "Named invitations for Tanzanian events",
  heroTitle: "Bring every guest",
  heroTitleEnd: "to the door.",
  heroBody:
    "Design one card, set each guest’s name, send it on WhatsApp — SMS and email where the package includes them — and check them in at the door.",
  seeHow: "See how it works",
  seePricing: "See packages",
  continue: "Create an Invitation",
  startCard: "Start this card",
  proofNamed: "Named cards",
  proofNamedBody: "Every guest prints with their own line.",
  proofWhatsApp: "WhatsApp first",
  proofWhatsAppBody: "SMS and email on the packages that include them.",
  proofDoor: "QR at the door",
  proofDoorBody: "The card is the pass. Scan once.",
  proofTzs: "Priced in TZS",
  proofTzsBody: "One price per event, by list size.",
  howEyebrow: "How it works",
  howTitle: "Three steps from the card to the door.",
  howBody: "One design. A named invitation for every guest. A scan when they arrive.",
  howStep1Title: "Choose the card",
  howStep1Body:
    "Weddings, send-offs, graduations, church, and the rest of the Tanzanian set. One design for the event.",
  howStep2Title: "Set each guest’s name",
  howStep2Body: "The list writes the name onto the same stock. Nobody shares a reprint.",
  howStep3Title: "Send, then scan",
  howStep3Body: "WhatsApp first. SMS and email where the package includes them. The QR is the pass.",
  featuresEyebrow: "On the desk",
  featuresTitle: "Everything the door list needs.",
  featureNamedTitle: "Named, not duplicated",
  featureNamedBody: "John Mwita and Asha Kimaro leave with their own card. The event line stays. The name changes.",
  featureSendTitle: "Send your way",
  featureSendBody: "WhatsApp on Full press. SMS and email on House list. Email on every package.",
  featureDoorTitle: "The card is the door pass",
  featureDoorBody: "Each invitation carries a unique QR. The desk scans it once. A later scan returns already checked in.",
  featureBulkTitle: "The list does the press",
  featureBulkBody: "Bulk generate fills each card from the guest list. You do not redraw the design for each person.",
  cardsEyebrow: "Browse by occasion",
  cardsTitle: "The stock guests receive.",
  cardsBody:
    "Weddings, send-offs, birthdays, graduations, church, corporate, conferences, and celebrations — composed here, not taken from a live job.",
  packagesEyebrow: "Simple pricing",
  packagesTitle: "Packages",
  packagesBody: "One price per event. You pay for the size of the list and the channels we send on. Figures in TZS.",
  packagesNote:
    "Catalog prices for the desk. You confirm the job when you create the invitation — this is not a live charge.",
  usualJob: "Most chosen",
  perEvent: "per event",
  choosePackage: "Choose this package",
  closeTitle: "Ready to set the list?",
  closeBody: "Pick the card, choose the package, send a named invitation to every guest.",
  footerMark: "InviteFlow",
  footerLine:
    "Invitation production for Tanzanian events that keep a door list. English now; Kiswahili when that locale is written.",
  heroDelivery: "WhatsApp first, then SMS and email.",
  priceFrom: "From",
} as const;

export type MessageKey = keyof typeof en;

const sw: Partial<Record<MessageKey, string>> = {};

export function t(key: MessageKey, language: InterfaceLanguage = MARKET.interfaceLanguage): string {
  if (language === "sw") {
    return sw[key] ?? en[key];
  }
  return en[key];
}
