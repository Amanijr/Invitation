const SESSION_KEY = "inviteflow-session";
const INTENT_KEY = "inviteflow-intent";
const ACTIVE_EVENT_KEY = "inviteflow-active-event";

export interface DeskIntent {
  packageId: string;
  templateId: string;
  templateName?: string;
  occasion?: string;
}

export interface Session {
  email: string;
  token: string;
  userId: string;
  firstName?: string;
  lastName?: string;
  role?: string;
}

export interface AuthResponse {
  token: string;
  tokenType?: string;
  userId: string;
  firstName?: string;
  lastName?: string;
  email: string;
  role?: string;
}

function scopedActiveEventKey(userId: string): string {
  return `${ACTIVE_EVENT_KEY}:${userId}`;
}

export function getSession(): Session | null {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<Session>;
    if (!parsed.email || !parsed.token || !parsed.userId) return null;
    return parsed as Session;
  } catch {
    return null;
  }
}

export function isSignedIn(): boolean {
  return getSession() !== null;
}

export function getToken(): string | null {
  return getSession()?.token ?? null;
}

export function getUserId(): string | null {
  return getSession()?.userId ?? null;
}

export function signIn(auth: AuthResponse): void {
  try {
    sessionStorage.removeItem(ACTIVE_EVENT_KEY);
  } catch {
    /* private mode */
  }
  const session: Session = {
    email: auth.email,
    token: auth.token,
    userId: auth.userId,
    firstName: auth.firstName,
    lastName: auth.lastName,
    role: auth.role,
  };
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  window.dispatchEvent(new Event("inviteflow-session"));
}

export function signOut(): void {
  localStorage.removeItem(SESSION_KEY);
  try {
    sessionStorage.removeItem(ACTIVE_EVENT_KEY);
  } catch {
    /* private mode */
  }
  window.dispatchEvent(new Event("inviteflow-session"));
}

export function setIntent(intent: DeskIntent): void {
  sessionStorage.setItem(INTENT_KEY, JSON.stringify(intent));
}

export function getIntent(): DeskIntent | null {
  try {
    const raw = sessionStorage.getItem(INTENT_KEY);
    if (!raw) return null;
    return JSON.parse(raw) as DeskIntent;
  } catch {
    return null;
  }
}

export function clearIntent(): void {
  sessionStorage.removeItem(INTENT_KEY);
}

export function getActiveEventId(): string | null {
  try {
    const userId = getUserId();
    if (!userId) return null;
    return sessionStorage.getItem(scopedActiveEventKey(userId));
  } catch {
    return null;
  }
}

export function setActiveEventId(id: string | null): void {
  const userId = getUserId();
  if (!userId) return;
  const key = scopedActiveEventKey(userId);
  if (!id) {
    sessionStorage.removeItem(key);
    return;
  }
  sessionStorage.setItem(key, id);
}

/** Keep a stored event only if it belongs to this user. */
export function syncActiveEventId(availableIds: string[]): string | null {
  const held = getActiveEventId();
  if (held && availableIds.includes(held)) return held;
  setActiveEventId(null);
  return null;
}

/** Prefer this user's stored event; otherwise the first event they own. */
export function syncActiveEventIdOrFirst(availableIds: string[]): string | null {
  const next = syncActiveEventId(availableIds) ?? availableIds[0] ?? null;
  setActiveEventId(next);
  return next;
}
