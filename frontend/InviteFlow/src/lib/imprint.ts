/** Formats that must match ECardRenderingEngineServiceImpl date/time on the press. */

export function formatImprintDate(iso: string | null | undefined): string {
  if (!iso) return "Date TBD";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "Date TBD";
  return date.toLocaleDateString("en-GB", { day: "numeric", month: "long", year: "numeric" });
}

export function formatImprintTime(iso: string | null | undefined): string {
  if (!iso) return "Time TBD";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "Time TBD";
  return date.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit", hour12: true });
}

export const DESIGNER_FONT_BASE_HEIGHT = 1080;
