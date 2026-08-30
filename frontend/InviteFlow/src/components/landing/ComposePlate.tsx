import { ChevronLeft, ChevronRight } from "lucide-react";
import type { ReactNode } from "react";
import { InvitationSample } from "@/components/landing/InvitationSample";
import { Button } from "@/components/ui/button";
import { CATALOG_TEMPLATES, type CatalogTemplate } from "@/lib/catalog";

export function ComposePlate({
  template,
  templates = CATALOG_TEMPLATES,
  reducedMotion,
  onChange,
  staged = false,
  afterCard,
}: {
  template: CatalogTemplate;
  templates?: CatalogTemplate[];
  reducedMotion: boolean;
  onChange: (template: CatalogTemplate) => void;
  staged?: boolean;
  afterCard?: ReactNode;
}) {
  const index = Math.max(0, templates.findIndex((item) => item.id === template.id));

  const cycle = (step: number) => {
    const next = templates[(index + step + templates.length) % templates.length];
    onChange(next);
  };

  return (
    <div className="flex flex-col items-center">
      <div className="w-full max-w-sm">
        <InvitationSample
          key={template.id}
          template={template}
          size="hero"
          staged={staged}
          reducedMotion={reducedMotion}
        />
      </div>

      {afterCard}

      <div className="mt-6 flex w-full max-w-sm items-center justify-between gap-2">
        <Button type="button" variant="ghost" size="icon" aria-label="Previous sample" onClick={() => cycle(-1)}>
          <ChevronLeft />
        </Button>
        <p className="font-mono text-xs tabular-nums text-muted-foreground">
          {String(index + 1).padStart(2, "0")} / {String(templates.length).padStart(2, "0")}
          <span className="mt-1 block text-center font-sans text-sm font-medium text-foreground">{template.name}</span>
        </p>
        <Button type="button" variant="ghost" size="icon" aria-label="Next sample" onClick={() => cycle(1)}>
          <ChevronRight />
        </Button>
      </div>
    </div>
  );
}
