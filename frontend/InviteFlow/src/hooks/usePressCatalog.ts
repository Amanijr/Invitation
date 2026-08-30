import { useEffect, useMemo, useState } from "react";
import { apiFetch, readJson, templateCoverSrc } from "@/lib/api";
import {
  CATALOG_TEMPLATES,
  eventTypeToOccasion,
  occasionLabel,
  type CatalogTemplate,
  type Occasion,
} from "@/lib/catalog";

interface PressTemplate {
  id: string;
  templateName: string;
  eventType: string;
  active?: boolean;
  fileUrl?: string;
  storagePath?: string;
  previewImageUrl?: string;
  content?: string;
  createdAt?: string;
}

function parseAttribution(content?: string): { license?: string; sourceUrl?: string } {
  if (!content) return {};
  try {
    const parsed = JSON.parse(content) as { license?: string; pageUrl?: string };
    return {
      license: typeof parsed.license === "string" ? parsed.license : undefined,
      sourceUrl: typeof parsed.pageUrl === "string" ? parsed.pageUrl : undefined,
    };
  } catch {
    return {};
  }
}

function toCatalog(item: PressTemplate): CatalogTemplate {
  const occasion = eventTypeToOccasion(item.eventType);
  const sample = CATALOG_TEMPLATES.find((card) => card.occasion === occasion) ?? CATALOG_TEMPLATES[0];
  const attribution = parseAttribution(item.content);
  return {
    ...sample,
    id: item.id,
    name: item.templateName,
    occasion,
    occasionLabel: occasionLabel(occasion),
    fileUrl: templateCoverSrc(item),
    sourceUrl: attribution.sourceUrl,
    license: attribution.license,
    developmentExample: false,
  };
}

export function usePressCatalog() {
  const [press, setPress] = useState<CatalogTemplate[]>([]);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const res = await apiFetch("/templates/active");
        if (!res.ok) return;
        const data = await readJson<PressTemplate[]>(res);
        if (cancelled) return;
        setPress(
          data
            .filter((item) => item.active !== false)
            .sort((a, b) => (b.createdAt || "").localeCompare(a.createdAt || ""))
            .map(toCatalog)
        );
      } catch {
        /* composed samples remain */
      } finally {
        if (!cancelled) setLoaded(true);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const templates = useMemo(
    () => (press.length > 0 ? press : CATALOG_TEMPLATES),
    [press]
  );

  return { templates, fromPress: press.length > 0, loaded };
}

export function findTemplate(
  templates: CatalogTemplate[],
  id: string,
  occasion?: "ALL" | Occasion
): CatalogTemplate {
  const pool = occasion && occasion !== "ALL" ? templates.filter((item) => item.occasion === occasion) : templates;
  return pool.find((item) => item.id === id) ?? pool[0] ?? templates[0] ?? CATALOG_TEMPLATES[0];
}
