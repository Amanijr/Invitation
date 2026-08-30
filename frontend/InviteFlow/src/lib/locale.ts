/** Tanzanian market defaults. Interface copy stays English until Kiswahili is authored. */
export const MARKET = {
  interfaceLanguage: "en" as InterfaceLanguage,
  locale: "en-TZ",
  currency: "TZS",
  callingCode: "+255",
} as const;

export const INTERFACE_LANGUAGES = [
  { id: "en", label: "English", htmlLang: "en-TZ" },
  { id: "sw", label: "Kiswahili", htmlLang: "sw-TZ" },
] as const;

export type InterfaceLanguage = (typeof INTERFACE_LANGUAGES)[number]["id"];

/** Sample guest line used on the landing. Matches desk normalization to +255. */
export const SAMPLE_MSISDN = "0754221018";

export function formatMoney(amount: number): string {
  return new Intl.NumberFormat(MARKET.locale, {
    style: "currency",
    currency: MARKET.currency,
    currencyDisplay: "code",
    maximumFractionDigits: 0,
  }).format(amount);
}

/** Display a Tanzanian mobile as +255 7XX XXX XXX. Leaves unknown shapes unchanged. */
export function formatPhone(input: string): string {
  const digits = input.replace(/\D/g, "");
  let national = digits;
  if (digits.startsWith("255") && digits.length >= 12) {
    national = digits.slice(3);
  } else if (digits.startsWith("0") && digits.length === 10) {
    national = digits.slice(1);
  }
  if (national.length !== 9) return input;
  return `${MARKET.callingCode} ${national.slice(0, 3)} ${national.slice(3, 6)} ${national.slice(6)}`;
}

export function htmlLangFor(language: InterfaceLanguage = MARKET.interfaceLanguage): string {
  return INTERFACE_LANGUAGES.find((item) => item.id === language)?.htmlLang ?? "en-TZ";
}
