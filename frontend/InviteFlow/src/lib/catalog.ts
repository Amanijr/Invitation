/** Desk event kinds — English labels, Tanzanian occasion set. Not wedding-only. */
export const EVENT_TYPES = [
  { id: "WEDDING", label: "Wedding" },
  { id: "SEND_OFF", label: "Send-off" },
  { id: "BIRTHDAY", label: "Birthday" },
  { id: "GRADUATION", label: "Graduation" },
  { id: "CHURCH", label: "Church" },
  { id: "ANNIVERSARY", label: "Anniversary" },
  { id: "CORPORATE", label: "Corporate" },
  { id: "CONFERENCE", label: "Conference" },
  { id: "SEMINAR", label: "Seminar" },
  { id: "MEETING", label: "Meeting" },
  { id: "PARTY", label: "Party" },
  { id: "CELEBRATION", label: "Celebration" },
  { id: "GALA", label: "Gala" },
  { id: "EXPO", label: "Expo" },
  { id: "FUNERAL", label: "Memorial" },
  { id: "OTHER", label: "Other" },
] as const;

export type EventTypeId = (typeof EVENT_TYPES)[number]["id"];

export function occasionToEventType(occasion: Occasion): EventTypeId {
  if (occasion === "CELEBRATION") return "CELEBRATION";
  return occasion as EventTypeId;
}

export type Occasion =
  | "WEDDING"
  | "SEND_OFF"
  | "BIRTHDAY"
  | "GRADUATION"
  | "CHURCH"
  | "CORPORATE"
  | "CONFERENCE"
  | "CELEBRATION"
  | "FUNERAL"
  | "OTHER";

export type SampleTone =
  | "ivory"
  | "slate"
  | "carmine"
  | "navy"
  | "linen"
  | "charcoal"
  | "sand"
  | "grove"
  | "sanctuary"
  | "festal";

export interface CatalogTemplate {
  id: string;
  name: string;
  occasion: Occasion;
  occasionLabel: string;
  blurb: string;
  guest: string;
  hosts?: string;
  line?: string;
  date: string;
  venue: string;
  tone: SampleTone;
  /** Landing cards are composed samples, not files from a live press. */
  developmentExample: boolean;
  /** Press file from the API when this card was seeded or uploaded. */
  fileUrl?: string;
  sourceUrl?: string;
  license?: string;
}

export interface CatalogPackage {
  id: string;
  name: string;
  priceFrom: number;
  /** Short commercial line under the name — InviteDrop-style “for this size of event”. */
  catchphrase: string;
  summary: string;
  guests: string;
  channels: string[];
  includes: string[];
  recommended?: boolean;
}

export const CATALOG_TEMPLATES: CatalogTemplate[] = [
  {
    id: "gold-wedding",
    name: "Gold wedding card",
    occasion: "WEDDING",
    occasionLabel: "Wedding",
    blurb: "Ivory stock, guest name, and a QR for the door.",
    hosts: "Amani & Neema",
    guest: "John Mwita",
    date: "12 September 2026",
    venue: "The Slipway, Dar es Salaam",
    tone: "ivory",
    developmentExample: true,
  },
  {
    id: "mbezi-send-off",
    name: "Mbezi send-off",
    occasion: "SEND_OFF",
    occasionLabel: "Send-off",
    blurb: "Warm stock for a ladies’ sitting and a named door list.",
    hosts: "Zahra Hassan",
    guest: "Fatma Ali",
    line: "Send-off · ladies sitting",
    date: "8 August 2026",
    venue: "Mbezi Garden, Dar es Salaam",
    tone: "sand",
    developmentExample: true,
  },
  {
    id: "letterpress-birthday",
    name: "Letterpress birthday",
    occasion: "BIRTHDAY",
    occasionLabel: "Birthday",
    blurb: "Carmine rule and a short toast line.",
    guest: "Imani Juma",
    line: "Thirty years",
    date: "18 April 2026",
    venue: "Njiro, Arusha",
    tone: "carmine",
    developmentExample: true,
  },
  {
    id: "nkrumah-graduation",
    name: "Nkrumah hall pass",
    occasion: "GRADUATION",
    occasionLabel: "Graduation",
    blurb: "Class line, graduate name, and a scan for the hall.",
    hosts: "Class of 2026",
    guest: "Baraka Mushi",
    line: "Bachelor of Arts",
    date: "21 November 2026",
    venue: "Nkrumah Hall, University of Dar es Salaam",
    tone: "grove",
    developmentExample: true,
  },
  {
    id: "cathedral-notice",
    name: "Cathedral notice",
    occasion: "CHURCH",
    occasionLabel: "Church",
    blurb: "Service card with family name and a door QR.",
    hosts: "Harvest thanksgiving",
    guest: "The Mwakyusa family",
    line: "Service at 9:00",
    date: "Sunday 15 November 2026",
    venue: "St. Joseph's Cathedral, Dar es Salaam",
    tone: "sanctuary",
    developmentExample: true,
  },
  {
    id: "gala-notice",
    name: "Gala notice",
    occasion: "CORPORATE",
    occasionLabel: "Corporate",
    blurb: "Navy plate for a seated evening.",
    guest: "Asha Kimaro",
    date: "22 November 2026",
    venue: "Kilimanjaro Hotel, Dar es Salaam",
    tone: "navy",
    developmentExample: true,
  },
  {
    id: "summit-pass",
    name: "Summit pass",
    occasion: "CONFERENCE",
    occasionLabel: "Conference",
    blurb: "Steel rule, session block, scannable credential.",
    guest: "Neema Lyimo",
    line: "Dar Tech Forum",
    date: "4–6 March 2026",
    venue: "Julius Nyerere Convention Centre",
    tone: "slate",
    developmentExample: true,
  },
  {
    id: "coco-celebration",
    name: "Coco Beach card",
    occasion: "CELEBRATION",
    occasionLabel: "Celebration",
    blurb: "Open celebration stock with a named guest line.",
    hosts: "The Juma family",
    guest: "Amina Juma",
    line: "House celebration",
    date: "3 October 2026",
    venue: "Coco Beach, Dar es Salaam",
    tone: "festal",
    developmentExample: true,
  },
  {
    id: "memorial-card",
    name: "Memorial card",
    occasion: "FUNERAL",
    occasionLabel: "Memorial",
    blurb: "Quiet linen, name, and time of gathering.",
    guest: "The Mwakasege family",
    date: "9 January 2026",
    venue: "St. Peter's, Dodoma",
    tone: "linen",
    developmentExample: true,
  },
  {
    id: "expo-ticket",
    name: "Expo ticket",
    occasion: "OTHER",
    occasionLabel: "Expo",
    blurb: "Compact ticket with a scan field.",
    guest: "Guest 1482",
    date: "30 May 2026",
    venue: "Mwalimu Nyerere Grounds, Dar es Salaam",
    tone: "charcoal",
    developmentExample: true,
  },
];

export const CATALOG_PACKAGES: CatalogPackage[] = [
  {
    id: "single-sitting",
    name: "Single sitting",
    priceFrom: 180,
    catchphrase: "For an intimate list.",
    summary: "One occasion, a short list, email only.",
    guests: "Up to 50 guests",
    channels: ["Email"],
    includes: [
      "Up to 50 named cards",
      "Email delivery",
      "QR on every card",
      "Door check-in",
    ],
  },
  {
    id: "house-list",
    name: "House list",
    priceFrom: 640,
    catchphrase: "For the list the house already keeps.",
    summary: "The usual desk job: list, press, SMS and email.",
    guests: "Up to 250 guests",
    channels: ["SMS", "Email"],
    includes: [
      "Up to 250 named cards",
      "SMS and email",
      "Bulk generate from the guest list",
      "Delivery log",
      "QR and door check-in",
    ],
    recommended: true,
  },
  {
    id: "full-press",
    name: "Full press",
    priceFrom: 1800,
    catchphrase: "For the hall, the phone, and the scan.",
    summary: "Large room, WhatsApp, SMS, email, and the door scan.",
    guests: "Up to 1,000 guests",
    channels: ["WhatsApp", "SMS", "Email"],
    includes: [
      "Up to 1,000 named cards",
      "WhatsApp, SMS, and email",
      "Designer fields",
      "Door scanner",
      "Check-in log",
    ],
  },
];

/** Hero occasion tiles — three live samples, not lifestyle photography. */
export const FEATURED_OCCASIONS = [
  {
    occasion: "WEDDING" as Occasion,
    templateId: "gold-wedding",
    kicker: "Wedding",
    title: "A named card for the sitting",
    body: "Ivory stock, guest line, and a QR for The Slipway door.",
  },
  {
    occasion: "SEND_OFF" as Occasion,
    templateId: "mbezi-send-off",
    kicker: "Send-off",
    title: "The ladies’ sitting, named",
    body: "Warm stock for Mbezi Garden and the door list.",
  },
  {
    occasion: "GRADUATION" as Occasion,
    templateId: "nkrumah-graduation",
    kicker: "Graduation",
    title: "A hall pass with a name",
    body: "Class line, graduate, and a scan at Nkrumah Hall.",
  },
] as const;

export const OCCASION_FILTERS: { id: "ALL" | Occasion; label: string }[] = [
  { id: "ALL", label: "All cards" },
  { id: "WEDDING", label: "Weddings" },
  { id: "SEND_OFF", label: "Send-offs" },
  { id: "BIRTHDAY", label: "Birthdays" },
  { id: "GRADUATION", label: "Graduations" },
  { id: "CHURCH", label: "Church" },
  { id: "CORPORATE", label: "Corporate" },
  { id: "CONFERENCE", label: "Conferences" },
  { id: "CELEBRATION", label: "Celebrations" },
];

export function eventTypeToOccasion(eventType: string): Occasion {
  const map: Record<string, Occasion> = {
    WEDDING: "WEDDING",
    SEND_OFF: "SEND_OFF",
    BIRTHDAY: "BIRTHDAY",
    GRADUATION: "GRADUATION",
    CHURCH: "CHURCH",
    CORPORATE: "CORPORATE",
    CONFERENCE: "CONFERENCE",
    PARTY: "CELEBRATION",
    CELEBRATION: "CELEBRATION",
    FUNERAL: "FUNERAL",
    ANNIVERSARY: "CELEBRATION",
    GALA: "CORPORATE",
    EXPO: "OTHER",
    SEMINAR: "CONFERENCE",
    MEETING: "CONFERENCE",
    OTHER: "OTHER",
  };
  return map[eventType] ?? "OTHER";
}

export function occasionLabel(occasion: Occasion): string {
  const fromFilter = OCCASION_FILTERS.find((item) => item.id === occasion);
  if (fromFilter) return fromFilter.label.replace(/s$/, "");
  return EVENT_TYPES.find((item) => item.id === occasion)?.label ?? occasion;
}

export function getTemplate(id: string, extras: CatalogTemplate[] = []): CatalogTemplate | undefined {
  return extras.find((item) => item.id === id) ?? CATALOG_TEMPLATES.find((item) => item.id === id);
}

export function getPackage(id: string): CatalogPackage | undefined {
  return CATALOG_PACKAGES.find((item) => item.id === id);
}
