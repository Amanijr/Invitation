import type { SampleTone } from "@/lib/catalog";

/**
 * Event / invitation palettes. These must not leak into application chrome.
 * Brand tokens live in index.css (midnight, ivory, gold).
 */
export interface EventTheme {
  paper: string;
  ink: string;
  rule: string;
  muted: string;
}

export const EVENT_THEMES: Record<SampleTone, EventTheme> = {
  ivory: { paper: "#f8f5ef", ink: "#111318", rule: "#c9a227", muted: "#5f6268" },
  slate: { paper: "#e8eaed", ink: "#111318", rule: "#2c5f4f", muted: "#5f6268" },
  carmine: { paper: "#f7f1ee", ink: "#111318", rule: "#a34e3f", muted: "#6b4a46" },
  navy: { paper: "#111318", ink: "#f8f5ef", rule: "#c9a227", muted: "#9a9ca1" },
  linen: { paper: "#efece6", ink: "#111318", rule: "#8a8478", muted: "#6b6560" },
  charcoal: { paper: "#1c1e24", ink: "#f8f5ef", rule: "#c9a227", muted: "#9a9ca1" },
  sand: { paper: "#f3e6d8", ink: "#111318", rule: "#a34e3f", muted: "#6b4a46" },
  grove: { paper: "#eef2ee", ink: "#111318", rule: "#2c5f4f", muted: "#5f6268" },
  sanctuary: { paper: "#f4efe8", ink: "#111318", rule: "#6b3a3a", muted: "#6b6560" },
  festal: { paper: "#f6ebe6", ink: "#111318", rule: "#a34e3f", muted: "#6b4a46" },
};

export function getEventTheme(tone: SampleTone): EventTheme {
  return EVENT_THEMES[tone];
}
