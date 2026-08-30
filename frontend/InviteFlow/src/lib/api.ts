import { getSession, getToken, getUserId, signOut } from "./session";

export async function readJson<T>(res: Response): Promise<T> {
  const ct = res.headers.get("content-type") ?? "";
  if (!ct.includes("application/json")) {
    throw new Error("Could not reach the API. Confirm the server is running.");
  }
  try {
    return (await res.json()) as T;
  } catch {
    throw new Error("Could not reach the API. Confirm the server is running.");
  }
}

export async function readError(res: Response): Promise<string> {
  const ct = res.headers.get("content-type") ?? "";
  if (ct.includes("application/json")) {
    try {
      const body = (await res.json()) as {
        message?: string;
        error?: string;
        errors?: Record<string, string>;
      };
      if (typeof body.message === "string" && body.message.trim()) return body.message;
      if (body.errors && typeof body.errors === "object") {
        const fields = Object.values(body.errors).filter(
          (value): value is string => typeof value === "string" && value.trim().length > 0
        );
        if (fields.length) return fields.join(" ");
      }
      if (typeof body.error === "string" && body.error.trim()) return body.error;
    } catch {
      /* fall through */
    }
  } else {
    try {
      const text = await res.text();
      if (text.trim()) return text;
    } catch {
      /* fall through */
    }
  }
  if (res.status === 401) return "Sign in to continue.";
  if (res.status === 403) return "You do not have permission for this action.";
  if (res.status === 404) return "That record was not found.";
  return `Request failed (${res.status}).`;
}

const API_PREFIX = "/api/v1";

export function apiUrl(path: string): string {
  if (path.startsWith("/api/")) return path;
  const trimmed = path.startsWith("/") ? path : `/${path}`;
  return `${API_PREFIX}${trimmed}`;
}

export function templateFileUrl(templateId: string): string {
  return apiUrl(`/templates/${templateId}/file`);
}

/** Cover image only when a stored file or an external preview exists — never hit /file blindly. */
export function templateCoverSrc(template: {
  id: string;
  storagePath?: string | null;
  fileUrl?: string | null;
  previewImageUrl?: string | null;
}): string | undefined {
  if (template.storagePath) {
    if (template.fileUrl && /^https?:\/\//i.test(template.fileUrl)) return template.fileUrl;
    if (template.fileUrl?.startsWith("/api/")) return template.fileUrl;
    return templateFileUrl(template.id);
  }
  const preview = template.previewImageUrl;
  if (preview && /^https?:\/\//i.test(preview)) return preview;
  return undefined;
}

export function eventsListPath(): string {
  const userId = getUserId();
  return userId ? `/events/creator/${userId}` : "/events";
}

function isFormDataBody(body: BodyInit | null | undefined): boolean {
  return typeof FormData !== "undefined" && body instanceof FormData;
}

function isPublicAuthPath(path: string): boolean {
  const url = apiUrl(path);
  return url.includes("/api/v1/auth/login") || url.includes("/api/v1/auth/register");
}

export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  const token = getToken();
  if (token && !headers.has("Authorization") && !isPublicAuthPath(path)) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  if (init.body && !isFormDataBody(init.body) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const res = await fetch(apiUrl(path), { ...init, headers });

  if (res.status === 401 && !apiUrl(path).includes("/api/v1/auth/")) {
    const session = getSession();
    if (session?.token) signOut();
  }

  return res;
}

export async function apiJson<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await apiFetch(path, init);
  if (!res.ok) {
    throw new Error(await readError(res));
  }
  return readJson<T>(res);
}
