/**
 * Motion philosophy — Phase A (do not add decorative animation).
 *
 * duration   hover 150ms · press 100ms · entrance 400ms · exit 240ms
 * easing     enter ease-out · exit ease-in · color/opacity only for chrome
 * spring     critically damped, bounce 0 (uiSpring)
 * hover      color or elevation, never travel
 * press      scale 0.97 via .press
 * entrance   reserved for later phases; opacity + 8px y, same spring
 * scroll     not used until a later phase
 * reduced    opacity only, no transform
 *
 * Motion for React is already in the stack. Do not add another animation library.
 */
export const motionDuration = {
  hover: 0.15,
  press: 0.1,
  entrance: 0.4,
  exit: 0.24,
} as const;

export function project(initialVelocity: number, decelerationRate = 0.998): number {
  return ((initialVelocity / 1000) * decelerationRate) / (1 - decelerationRate);
}

export function projectEndpoint(current: number, velocity: number, decelerationRate = 0.998): number {
  return current + project(velocity, decelerationRate);
}

export function rubberband(overshoot: number, dimension: number, constant = 0.55): number {
  return (overshoot * dimension * constant) / (dimension + constant * Math.abs(overshoot));
}

export function prefersReducedMotion(): boolean {
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

export type PointSample = { t: number; x: number; y: number };

export function velocityFromHistory(history: PointSample[]): { vx: number; vy: number } {
  if (history.length < 2) return { vx: 0, vy: 0 };
  const a = history[0];
  const b = history[history.length - 1];
  const dt = (b.t - a.t) / 1000;
  if (dt <= 0) return { vx: 0, vy: 0 };
  return { vx: (b.x - a.x) / dt, vy: (b.y - a.y) / dt };
}

export const uiSpring = { type: "spring" as const, bounce: 0, duration: 0.4 };

export const sheetSpring = { type: "spring" as const, bounce: 0, duration: 0.3 };

export const flickSpring = { type: "spring" as const, bounce: 0.2, duration: 0.3 };
