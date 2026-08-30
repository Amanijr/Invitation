import { type PointerEvent, useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, QrCode, Type } from "lucide-react";
import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { project, rubberband, velocityFromHistory, type PointSample } from "@/lib/physics";
import { apiFetch, readJson, templateCoverSrc } from "@/lib/api";
import { DESIGNER_FONT_BASE_HEIGHT } from "@/lib/imprint";
import { cn } from "@/lib/utils";
import { toast } from "sonner";

export type FieldType = "GUEST_NAME" | "QR_CODE" | "EVENT_NAME" | "EVENT_DATE" | "EVENT_TIME" | "EVENT_VENUE";

export interface TemplateFieldConfig {
  id?: string;
  templateId?: string;
  fieldType: FieldType;
  x: number;
  y: number;
  width: number;
  height: number;
  fontSize: number;
  fontColor: string;
  alignment: "LEFT" | "CENTER" | "RIGHT";
  fontWeight: "NORMAL" | "BOLD";
  fontFamily?: string;
  qrSize?: number;
  sampleText?: string;
}

export interface TemplateDetails {
  id: string;
  templateName: string;
  eventType: string;
  width?: number;
  height?: number;
  originalFileName?: string;
  storagePath?: string;
  fileUrl?: string;
  previewImageUrl?: string;
}

const PRESS_FIELDS = new Set<FieldType>(["GUEST_NAME", "QR_CODE"]);
const DEFAULT_NAME = "Ms. Mama Swai";

const AVAILABLE_FIELDS: { type: FieldType; label: string; defaultW: number; defaultH: number }[] = [
  { type: "GUEST_NAME", label: "Guest name", defaultW: 76, defaultH: 8 },
  { type: "QR_CODE", label: "QR code", defaultW: 24, defaultH: 18 },
];

type ResizeHandle = "n" | "s" | "e" | "w" | "ne" | "nw" | "se" | "sw";

const HANDLES: { id: ResizeHandle; className: string; label: string }[] = [
  { id: "nw", className: "left-0 top-0 -translate-x-1/2 -translate-y-1/2 cursor-nwse-resize", label: "Resize top left" },
  { id: "n", className: "left-1/2 top-0 -translate-x-1/2 -translate-y-1/2 cursor-ns-resize", label: "Resize top" },
  { id: "ne", className: "right-0 top-0 translate-x-1/2 -translate-y-1/2 cursor-nesw-resize", label: "Resize top right" },
  { id: "e", className: "right-0 top-1/2 translate-x-1/2 -translate-y-1/2 cursor-ew-resize", label: "Resize right" },
  { id: "se", className: "bottom-0 right-0 translate-x-1/2 translate-y-1/2 cursor-nwse-resize", label: "Resize bottom right" },
  { id: "s", className: "bottom-0 left-1/2 -translate-x-1/2 translate-y-1/2 cursor-ns-resize", label: "Resize bottom" },
  { id: "sw", className: "bottom-0 left-0 -translate-x-1/2 translate-y-1/2 cursor-nesw-resize", label: "Resize bottom left" },
  { id: "w", className: "left-0 top-1/2 -translate-x-1/2 -translate-y-1/2 cursor-ew-resize", label: "Resize left" },
];

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value));
}

function round1(value: number) {
  return Math.round(value * 10) / 10;
}

function applyResize(
  start: Pick<TemplateFieldConfig, "x" | "y" | "width" | "height">,
  handle: ResizeHandle,
  dxPct: number,
  dyPct: number,
  lockAspect: boolean
) {
  let { x, y, width, height } = start;
  const aspect = start.width / Math.max(start.height, 0.1);

  if (handle.includes("e")) width = start.width + dxPct;
  if (handle.includes("w")) {
    width = start.width - dxPct;
    x = start.x + dxPct;
  }
  if (handle.includes("s")) height = start.height + dyPct;
  if (handle.includes("n")) {
    height = start.height - dyPct;
    y = start.y + dyPct;
  }

  if (lockAspect) {
    if (handle === "e" || handle === "w") {
      height = width / aspect;
      y = start.y + (start.height - height) / 2;
    } else if (handle === "n" || handle === "s") {
      width = height * aspect;
      x = start.x + (start.width - width) / 2;
    } else {
      const nextAspect = width / Math.max(height, 0.1);
      if (nextAspect > aspect) height = width / aspect;
      else width = height * aspect;
      if (handle.includes("w")) x = start.x + start.width - width;
      if (handle.includes("n")) y = start.y + start.height - height;
    }
  }

  const minW = 5;
  const minH = 3;
  if (width < minW) {
    if (handle.includes("w")) x = start.x + start.width - minW;
    width = minW;
  }
  if (height < minH) {
    if (handle.includes("n")) y = start.y + start.height - minH;
    height = minH;
  }

  if (x < 0) {
    width += x;
    x = 0;
  }
  if (y < 0) {
    height += y;
    y = 0;
  }
  if (x + width > 100) width = 100 - x;
  if (y + height > 100) height = 100 - y;

  return { x: round1(x), y: round1(y), width: round1(width), height: round1(height) };
}

function scaleBox(field: TemplateFieldConfig, factor: number): TemplateFieldConfig {
  const cx = field.x + field.width / 2;
  const cy = field.y + field.height / 2;
  let width = clamp(field.width * factor, 5, 100);
  let height = clamp(field.height * factor, 3, 100);
  if (field.fieldType === "QR_CODE") {
    const size = Math.min(width, height);
    width = size;
    height = size;
  }
  let x = cx - width / 2;
  let y = cy - height / 2;
  x = clamp(x, 0, 100 - width);
  y = clamp(y, 0, 100 - height);
  return {
    ...field,
    x: round1(x),
    y: round1(y),
    width: round1(width),
    height: round1(height),
    fontSize: Math.round(clamp(field.fontSize * factor, 12, 120)),
  };
}

type Gesture =
  | {
      kind: "move";
      index: number;
      grabX: number;
      grabY: number;
      pointerId: number;
      committed: boolean;
      history: PointSample[];
    }
  | {
      kind: "resize";
      index: number;
      handle: ResizeHandle;
      pointerId: number;
      start: TemplateFieldConfig;
      originX: number;
      originY: number;
      lockAspect: boolean;
    }
  | {
      kind: "pinch";
      index: number;
      pointers: Map<number, { x: number; y: number }>;
      startDist: number;
      start: TemplateFieldConfig;
    };

function FieldInspector({
  field,
  index,
  onChange,
  onRemove,
}: {
  field: TemplateFieldConfig;
  index: number;
  onChange: (key: keyof TemplateFieldConfig, value: string | number) => void;
  onRemove: (index: number) => void;
}) {
  const isQr = field.fieldType === "QR_CODE";
  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="font-mono text-xs text-muted-foreground">{isQr ? "QR code" : "Guest name"}</p>
          <h3 className="font-display text-lg font-semibold">Edit slot</h3>
        </div>
        <Button variant="outline" size="sm" onClick={() => onRemove(index)}>
          Remove
        </Button>
      </div>
      <p className="text-xs leading-5 text-muted-foreground">
        Drag the slot to move it. Pull a handle or pinch to size it. {isQr ? "" : "Double-click the name on the card to type."}
      </p>
      {!isQr ? (
        <>
          <div className="space-y-2">
            <Label htmlFor="preview-name">Name on this card</Label>
            <Input
              id="preview-name"
              value={field.sampleText ?? ""}
              onChange={(e) => onChange("sampleText", e.target.value)}
              placeholder={DEFAULT_NAME}
            />
            <p className="text-xs leading-5 text-muted-foreground">
              This is the look. Generate still prints each guest’s real name from the list.
            </p>
          </div>
          <div className="space-y-2">
            <div className="flex justify-between text-sm">
              <Label>Type size</Label>
              <span className="font-mono text-xs">{field.fontSize}px</span>
            </div>
            <input
              type="range"
              min={12}
              max={96}
              value={field.fontSize ?? 32}
              onChange={(e) => onChange("fontSize", parseInt(e.target.value, 10))}
              className="w-full accent-[var(--accent)]"
            />
          </div>
          <div className="space-y-2">
            <Label>Color</Label>
            <div className="flex gap-2">
              <input
                type="color"
                value={field.fontColor || "#111318"}
                onChange={(e) => onChange("fontColor", e.target.value)}
                className="size-11 rounded-md border border-input"
              />
              <Input
                value={field.fontColor ?? ""}
                onChange={(e) => onChange("fontColor", e.target.value)}
                className="font-mono"
              />
            </div>
          </div>
          <div className="space-y-2">
            <Label>Align</Label>
            <div className="grid grid-cols-3 gap-2">
              {(["LEFT", "CENTER", "RIGHT"] as const).map((align) => (
                <Button
                  key={align}
                  type="button"
                  variant={field.alignment === align ? "secondary" : "outline"}
                  onClick={() => onChange("alignment", align)}
                >
                  {align.toLowerCase()}
                </Button>
              ))}
            </div>
          </div>
          <div className="space-y-2">
            <Label>Weight</Label>
            <div className="grid grid-cols-2 gap-2">
              {(["NORMAL", "BOLD"] as const).map((weight) => (
                <Button
                  key={weight}
                  type="button"
                  variant={field.fontWeight === weight ? "secondary" : "outline"}
                  onClick={() => onChange("fontWeight", weight)}
                >
                  {weight.toLowerCase()}
                </Button>
              ))}
            </div>
          </div>
        </>
      ) : (
        <p className="text-sm leading-6 text-muted-foreground">
          Keep the box on the empty QR area of the artwork. Hold Shift while dragging a handle to free the shape;
          otherwise it stays square.
        </p>
      )}
      <Separator />
      <div className="grid grid-cols-2 gap-3">
        {(
          [
            ["x", "X %"],
            ["y", "Y %"],
            ["width", "W %"],
            ["height", "H %"],
          ] as const
        ).map(([key, label]) => (
          <div key={key} className="space-y-1">
            <Label className="text-xs">{label}</Label>
            <Input
              type="number"
              step="0.1"
              value={field[key] ?? 0}
              onChange={(e) => onChange(key, parseFloat(e.target.value) || 0)}
            />
          </div>
        ))}
      </div>
    </div>
  );
}

export function TemplateDesignerPage() {
  const { templateId } = useParams<{ templateId: string }>();
  const navigate = useNavigate();
  const [template, setTemplate] = useState<TemplateDetails | null>(null);
  const [fields, setFields] = useState<TemplateFieldConfig[]>([]);
  const [selectedFieldIndex, setSelectedFieldIndex] = useState<number | null>(null);
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [mode, setMode] = useState<"DESIGN" | "PREVIEW">("DESIGN");
  const [previewImage, setPreviewImage] = useState<string | null>(null);
  const [loadingPreview, setLoadingPreview] = useState(false);
  const canvasRef = useRef<HTMLDivElement>(null);
  const [canvasHeight, setCanvasHeight] = useState(0);
  const [imageAspect, setImageAspect] = useState<string | null>(null);
  const gestureRef = useRef<Gesture | null>(null);
  const fieldsRef = useRef(fields);
  fieldsRef.current = fields;

  useEffect(() => {
    if (!templateId) return;
    const fetchData = async () => {
      setLoading(true);
      setError(null);
      try {
        const tRes = await apiFetch(`/templates/${templateId}`);
        if (!tRes.ok) throw new Error("Could not load template.");
        setTemplate(await readJson<TemplateDetails>(tRes));
        const fRes = await apiFetch(`/templates/${templateId}/fields`);
        if (fRes.ok) {
          const loaded = await readJson<TemplateFieldConfig[]>(fRes);
          setFields(
            loaded
              .filter((field) => PRESS_FIELDS.has(field.fieldType))
              .map((field) =>
                field.fieldType === "GUEST_NAME" && !field.sampleText?.trim()
                  ? { ...field, sampleText: DEFAULT_NAME }
                  : field
              )
          );
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : "Could not load designer.");
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [templateId]);

  useEffect(() => {
    if (mode !== "DESIGN") return;
    const el = canvasRef.current;
    if (!el) return;
    const measure = () => {
      const rect = el.getBoundingClientRect();
      setCanvasHeight(rect.height);
    };
    measure();
    const observer = new ResizeObserver(measure);
    observer.observe(el);
    return () => observer.disconnect();
  }, [mode, template, imageAspect]);

  useEffect(() => {
    const el = canvasRef.current;
    if (!el || mode !== "DESIGN") return;
    const onWheel = (event: WheelEvent) => {
      if (selectedFieldIndex === null) return;
      if (!event.ctrlKey && !event.metaKey) return;
      event.preventDefault();
      const factor = event.deltaY > 0 ? 0.96 : 1.04;
      setFields((prev) => {
        const next = [...prev];
        const target = next[selectedFieldIndex];
        if (!target) return prev;
        next[selectedFieldIndex] = scaleBox(target, factor);
        return next;
      });
    };
    el.addEventListener("wheel", onWheel, { passive: false });
    return () => el.removeEventListener("wheel", onWheel);
  }, [mode, selectedFieldIndex]);

  const handleAddField = (fieldType: FieldType) => {
    const existingIndex = fields.findIndex((f) => f.fieldType === fieldType);
    if (existingIndex >= 0) {
      setSelectedFieldIndex(existingIndex);
      return;
    }
    const fieldMeta = AVAILABLE_FIELDS.find((f) => f.type === fieldType);
    const updated = [
      ...fields,
      {
        templateId,
        fieldType,
        x: 25,
        y: 20 + fields.length * 10,
        width: fieldMeta?.defaultW ?? 40,
        height: fieldMeta?.defaultH ?? 8,
        fontSize: fieldType === "GUEST_NAME" ? 32 : 24,
        fontColor: "#111318",
        alignment: "CENTER" as const,
        fontWeight: "BOLD" as const,
        fontFamily: "SansSerif",
        qrSize: 200,
        sampleText: fieldType === "GUEST_NAME" ? DEFAULT_NAME : undefined,
      },
    ];
    setFields(updated);
    setSelectedFieldIndex(updated.length - 1);
  };

  const handleRemoveField = (index: number) => {
    setFields(fields.filter((_, i) => i !== index));
    setEditingIndex(null);
    if (selectedFieldIndex === index) setSelectedFieldIndex(null);
    else if (selectedFieldIndex !== null && selectedFieldIndex > index) {
      setSelectedFieldIndex(selectedFieldIndex - 1);
    }
  };

  const updateSelectedField = (key: keyof TemplateFieldConfig, value: string | number) => {
    if (selectedFieldIndex === null) return;
    const updated = [...fields];
    updated[selectedFieldIndex] = { ...updated[selectedFieldIndex], [key]: value };
    setFields(updated);
  };

  const handleSaveConfigs = async () => {
    if (!templateId) return;
    setSaving(true);
    try {
      const res = await apiFetch(`/templates/${templateId}/fields`, {
        method: "POST",
        body: JSON.stringify(fields),
      });
      if (!res.ok) throw new Error("Could not save field positions.");
      setFields(await readJson<TemplateFieldConfig[]>(res));
      setError(null);
      toast.success("Layout saved");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save.");
    } finally {
      setSaving(false);
    }
  };

  const handleFetchBackendPreview = async () => {
    if (!templateId) return;
    setLoadingPreview(true);
    try {
      const nameField = fields.find((field) => field.fieldType === "GUEST_NAME");
      const res = await apiFetch(`/templates/${templateId}/preview`, {
        method: "POST",
        body: JSON.stringify({
          guestName: nameField?.sampleText?.trim() || DEFAULT_NAME,
          sampleQrData: "INVITATION-TOKEN-SECURE-99482",
          fieldConfigs: fields,
        }),
      });
      if (!res.ok) throw new Error("Could not generate preview.");
      const data = await readJson<{ base64Image: string }>(res);
      setPreviewImage(data.base64Image);
      setMode("PREVIEW");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not generate preview.");
    } finally {
      setLoadingPreview(false);
    }
  };

  const beginMove = (e: PointerEvent<HTMLElement>, index: number) => {
    if (editingIndex === index) return;
    if (!canvasRef.current) return;
    const rect = canvasRef.current.getBoundingClientRect();
    const field = fields[index];
    const existing = gestureRef.current;
    if (existing?.kind === "move" && existing.index === index && existing.pointerId !== e.pointerId) {
      const pointers = new Map<number, { x: number; y: number }>([
        [existing.pointerId, { x: existing.history.at(-1)?.x ?? e.clientX, y: existing.history.at(-1)?.y ?? e.clientY }],
        [e.pointerId, { x: e.clientX, y: e.clientY }],
      ]);
      const [a, b] = [...pointers.values()];
      gestureRef.current = {
        kind: "pinch",
        index,
        pointers,
        startDist: Math.hypot(a.x - b.x, a.y - b.y) || 1,
        start: { ...field },
      };
      return;
    }
    const fieldPxX = (field.x / 100) * rect.width;
    const fieldPxY = (field.y / 100) * rect.height;
    gestureRef.current = {
      kind: "move",
      index,
      grabX: e.clientX - rect.left - fieldPxX,
      grabY: e.clientY - rect.top - fieldPxY,
      pointerId: e.pointerId,
      committed: false,
      history: [{ t: performance.now(), x: e.clientX, y: e.clientY }],
    };
  };

  const handlePointerDown = (e: PointerEvent<HTMLElement>, index: number, handle?: ResizeHandle) => {
    e.stopPropagation();
    if (editingIndex === index && !handle) return;
    e.preventDefault();
    setSelectedFieldIndex(index);
    setEditingIndex(null);
    try {
      (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
    } catch {
      /* untrusted or already released */
    }
    if (!canvasRef.current) return;
    const rect = canvasRef.current.getBoundingClientRect();
    const field = fields[index];
    if (handle) {
      gestureRef.current = {
        kind: "resize",
        index,
        handle,
        pointerId: e.pointerId,
        start: { ...field },
        originX: e.clientX - rect.left,
        originY: e.clientY - rect.top,
        lockAspect: field.fieldType === "QR_CODE" ? !e.shiftKey : e.shiftKey,
      };
      return;
    }
    beginMove(e, index);
  };

  const handlePointerMove = (e: PointerEvent<HTMLElement>) => {
    const g = gestureRef.current;
    if (!g || !canvasRef.current) return;
    const rect = canvasRef.current.getBoundingClientRect();
    const pointerX = e.clientX - rect.left;
    const pointerY = e.clientY - rect.top;

    if (g.kind === "pinch") {
      if (!g.pointers.has(e.pointerId)) return;
      g.pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
      if (g.pointers.size < 2) return;
      const [a, b] = [...g.pointers.values()];
      const dist = Math.hypot(a.x - b.x, a.y - b.y) || 1;
      const factor = dist / g.startDist;
      setFields((prev) => {
        const next = [...prev];
        next[g.index] = scaleBox(g.start, factor);
        return next;
      });
      return;
    }

    if (g.kind === "move") {
      if (g.pointerId !== e.pointerId) return;
      g.history.push({ t: performance.now(), x: e.clientX, y: e.clientY });
      if (g.history.length > 6) g.history.shift();
      if (!g.committed) {
        const origin = g.history[0];
        if (Math.hypot(e.clientX - origin.x, e.clientY - origin.y) < 10) return;
        g.committed = true;
      }
      setFields((prev) => {
        const updated = [...prev];
        const target = { ...updated[g.index] };
        let pctX = ((pointerX - g.grabX) / rect.width) * 100;
        let pctY = ((pointerY - g.grabY) / rect.height) * 100;
        const maxX = 100 - target.width;
        const maxY = 100 - target.height;
        if (pctX < 0) pctX = -rubberband(-pctX, 100) * 0.25;
        if (pctY < 0) pctY = -rubberband(-pctY, 100) * 0.25;
        if (pctX > maxX) pctX = maxX + rubberband(pctX - maxX, 100) * 0.25;
        if (pctY > maxY) pctY = maxY + rubberband(pctY - maxY, 100) * 0.25;
        target.x = round1(pctX);
        target.y = round1(pctY);
        updated[g.index] = target;
        return updated;
      });
      return;
    }

    if (g.pointerId !== e.pointerId) return;
    const dxPct = ((pointerX - g.originX) / rect.width) * 100;
    const dyPct = ((pointerY - g.originY) / rect.height) * 100;
    setFields((prev) => {
      const updated = [...prev];
      const box = applyResize(g.start, g.handle, dxPct, dyPct, g.lockAspect);
      const heightRatio = box.height / Math.max(g.start.height, 0.1);
      updated[g.index] = {
        ...g.start,
        ...box,
        fontSize: Math.round(clamp(g.start.fontSize * heightRatio, 12, 120)),
      };
      return updated;
    });
  };

  const handlePointerUp = (e: PointerEvent<HTMLElement>) => {
    const g = gestureRef.current;
    const canvas = canvasRef.current;
    if (g?.kind === "pinch") {
      g.pointers.delete(e.pointerId);
      if (g.pointers.size < 2) gestureRef.current = null;
      return;
    }
    if (g && canvas && g.kind === "move") {
      const rect = canvas.getBoundingClientRect();
      const { vx, vy } = velocityFromHistory(g.history);
      setFields((prev) => {
        const next = [...prev];
        const target = { ...next[g.index] };
        const pxX = (target.x / 100) * rect.width + project(vx);
        const pxY = (target.y / 100) * rect.height + project(vy);
        const pctX = (pxX / rect.width) * 100;
        const pctY = (pxY / rect.height) * 100;
        target.x = round1(clamp(pctX, 0, 100 - target.width));
        target.y = round1(clamp(pctY, 0, 100 - target.height));
        next[g.index] = target;
        return next;
      });
    } else if (g) {
      setFields((prev) => {
        const next = [...prev];
        const target = { ...next[g.index] };
        target.x = clamp(target.x, 0, 100 - target.width);
        target.y = clamp(target.y, 0, 100 - target.height);
        next[g.index] = target;
        return next;
      });
    }
    gestureRef.current = null;
  };

  const selectedField = selectedFieldIndex !== null ? fields[selectedFieldIndex] : null;

  if (loading) {
    return <p className="p-16 text-center text-sm text-muted-foreground">Loading designer…</p>;
  }

  if (error || !template) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4 p-6">
        <Alert variant="destructive" className="max-w-md">
          {error || "Template not found"}
        </Alert>
        <Button asChild variant="outline">
          <Link to="/templates">Return to templates</Link>
        </Button>
      </div>
    );
  }

  const inspector =
    selectedField && selectedFieldIndex !== null ? (
      <FieldInspector
        field={selectedField}
        index={selectedFieldIndex}
        onChange={updateSelectedField}
        onRemove={handleRemoveField}
      />
    ) : (
      <p className="text-sm leading-6 text-muted-foreground">
        Click the name or QR on the card. Drag to move, pull a handle or pinch to size, then save.
      </p>
    );

  return (
    <div className="flex h-[calc(100vh-4rem)] flex-col overflow-hidden">
      <header className="flex h-16 items-center justify-between gap-4 border-b border-border px-4">
        <div className="flex min-w-0 items-center gap-3">
          <Button variant="ghost" size="icon" onClick={() => navigate("/templates")} aria-label="Back to templates">
            <ArrowLeft className="size-4" />
          </Button>
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <Badge variant="secondary">{template.eventType}</Badge>
              <span className="font-mono text-xs text-muted-foreground">
                {template.width}×{template.height}px
              </span>
            </div>
            <h1 className="truncate font-display text-lg font-semibold">{template.templateName}</h1>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button variant={mode === "DESIGN" ? "secondary" : "ghost"} onClick={() => setMode("DESIGN")}>
            Canvas
          </Button>
          <Button variant={mode === "PREVIEW" ? "secondary" : "ghost"} disabled={loadingPreview} onClick={handleFetchBackendPreview}>
            {loadingPreview ? "Rendering…" : "Preview"}
          </Button>
          <Button onClick={handleSaveConfigs} disabled={saving}>
            {saving ? "Saving…" : "Save layout"}
          </Button>
        </div>
      </header>

      <div className="flex min-h-0 flex-1 flex-col lg:flex-row">
        <div className="flex min-h-0 min-w-0 flex-1">
        <aside className="hidden w-64 shrink-0 overflow-y-auto border-r border-border p-4 md:block">
          <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Fields</p>
          <p className="mt-1 text-xs leading-5 text-muted-foreground">
            The press stamps only the recipient name and a unique QR. Sit those two slots on the artwork, then save.
          </p>
          <div className="mt-4 space-y-2">
            {AVAILABLE_FIELDS.map((field) => {
              const isAdded = fields.some((f) => f.fieldType === field.type);
              return (
                <button
                  key={field.type}
                  type="button"
                  onClick={() => handleAddField(field.type)}
                  className={cn(
                    "flex h-auto w-full items-center gap-3 rounded-md border px-3 py-3 text-left text-sm transition-colors duration-150",
                    isAdded ? "border-accent bg-muted text-foreground" : "border-border hover:bg-muted"
                  )}
                >
                  {field.type === "QR_CODE" ? <QrCode className="size-4" /> : <Type className="size-4" />}
                  <span className="flex-1">{field.label}</span>
                  {isAdded ? <Badge variant="outline">On card</Badge> : null}
                </button>
              );
            })}
          </div>
        </aside>

        <main
          className="flex min-w-0 flex-1 items-center justify-center overflow-auto bg-muted/40 p-6"
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={handlePointerUp}
          onClick={() => {
            if (editingIndex !== null) return;
            setSelectedFieldIndex(null);
          }}
        >
          {mode === "DESIGN" ? (
            <div
              ref={canvasRef}
              className="relative overflow-visible rounded-md border border-border bg-card shadow-sm"
              style={{
                width: "100%",
                maxWidth: "900px",
                aspectRatio:
                  imageAspect ??
                  (template.width && template.height ? `${template.width}/${template.height}` : "3 / 4"),
              }}
            >
              <div className="absolute inset-0 overflow-hidden rounded-md">
                {templateCoverSrc(template) ? (
                  <img
                    src={templateCoverSrc(template)}
                    alt={template.templateName}
                    className="pointer-events-none h-full w-full object-contain"
                    onLoad={(e) => {
                      const img = e.currentTarget;
                      if (img.naturalWidth && img.naturalHeight) {
                        setImageAspect(`${img.naturalWidth} / ${img.naturalHeight}`);
                      }
                    }}
                    onError={(e) => {
                      (e.target as HTMLElement).style.display = "none";
                    }}
                  />
                ) : (
                  <div className="flex h-full items-center justify-center bg-muted text-sm text-muted-foreground">
                    Upload a finished card to place the name and QR.
                  </div>
                )}
              </div>
              {fields.map((field, idx) => {
                const isSelected = selectedFieldIndex === idx;
                const isEditing = editingIndex === idx;
                const displayName = field.sampleText?.trim() || DEFAULT_NAME;
                return (
                  <div
                    key={field.id ?? idx}
                    onClick={(e) => e.stopPropagation()}
                    onPointerDown={(e) => handlePointerDown(e, idx)}
                    onDoubleClick={(e) => {
                      if (field.fieldType !== "GUEST_NAME") return;
                      e.stopPropagation();
                      setSelectedFieldIndex(idx);
                      setEditingIndex(idx);
                      gestureRef.current = null;
                    }}
                    className={cn(
                      "absolute flex touch-none items-center justify-center",
                      isEditing ? "z-30 cursor-text" : "z-10 cursor-move select-none",
                      isSelected ? "z-20 ring-2 ring-accent" : "hover:ring-1 hover:ring-accent/50"
                    )}
                    style={{
                      left: `${field.x}%`,
                      top: `${field.y}%`,
                      width: `${field.width}%`,
                      height: `${field.height}%`,
                    }}
                  >
                    {field.fieldType === "QR_CODE" ? (
                      <div className="flex h-full w-full items-center justify-center bg-card/90 p-1">
                        <QrCode className="h-full w-full text-foreground" strokeWidth={1.5} />
                      </div>
                    ) : isEditing ? (
                      <textarea
                        autoFocus
                        value={field.sampleText ?? ""}
                        onChange={(e) => {
                          const updated = [...fieldsRef.current];
                          updated[idx] = { ...updated[idx], sampleText: e.target.value };
                          setFields(updated);
                        }}
                        onPointerDown={(e) => e.stopPropagation()}
                        onBlur={() => setEditingIndex(null)}
                        onKeyDown={(e) => {
                          if (e.key === "Escape" || (e.key === "Enter" && !e.shiftKey)) {
                            e.preventDefault();
                            setEditingIndex(null);
                          }
                        }}
                        className="h-full w-full resize-none bg-transparent px-1 outline-none"
                        style={{
                          color: field.fontColor || "#111318",
                          fontSize: `${Math.max(
                            8,
                            field.fontSize * ((canvasHeight || DESIGNER_FONT_BASE_HEIGHT) / DESIGNER_FONT_BASE_HEIGHT)
                          )}px`,
                          fontWeight: field.fontWeight === "BOLD" ? "bold" : "normal",
                          fontFamily: "sans-serif",
                          textAlign:
                            field.alignment === "LEFT" ? "left" : field.alignment === "RIGHT" ? "right" : "center",
                        }}
                      />
                    ) : (
                      <div
                        className="flex h-full w-full items-center overflow-hidden px-1"
                        style={{
                          justifyContent:
                            field.alignment === "LEFT"
                              ? "flex-start"
                              : field.alignment === "RIGHT"
                                ? "flex-end"
                                : "center",
                          color: field.fontColor || "#111318",
                          fontSize: `${Math.max(
                            8,
                            field.fontSize * ((canvasHeight || DESIGNER_FONT_BASE_HEIGHT) / DESIGNER_FONT_BASE_HEIGHT)
                          )}px`,
                          fontWeight: field.fontWeight === "BOLD" ? "bold" : "normal",
                          fontFamily: "sans-serif",
                          textAlign:
                            field.alignment === "LEFT" ? "left" : field.alignment === "RIGHT" ? "right" : "center",
                        }}
                      >
                        <span className="w-full leading-tight break-words">{displayName}</span>
                      </div>
                    )}
                    {isSelected && !isEditing ? (
                      <>
                        <div className="pointer-events-none absolute -top-5 left-0 rounded-sm bg-accent px-1.5 py-0.5 font-mono text-[10px] text-accent-foreground">
                          {field.fieldType === "QR_CODE" ? "QR" : "Name"}
                        </div>
                        {HANDLES.map((handle) => (
                          <div
                            key={handle.id}
                            role="slider"
                            aria-label={handle.label}
                            onPointerDown={(e) => handlePointerDown(e, idx, handle.id)}
                            className={cn(
                              "absolute z-30 flex size-6 items-center justify-center touch-none",
                              handle.className
                            )}
                          >
                            <span className="size-2.5 rounded-sm border-2 border-card bg-accent" />
                          </div>
                        ))}
                      </>
                    ) : null}
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="max-w-4xl rounded-md border border-border bg-card p-4">
              {previewImage ? (
                <img src={previewImage} alt="Card preview" className="max-h-[70vh] object-contain" />
              ) : (
                <p className="py-20 text-center text-sm text-muted-foreground">No preview yet.</p>
              )}
            </div>
          )}
        </main>
        </div>

        <aside className="max-h-56 shrink-0 overflow-y-auto border-t border-border p-4 lg:max-h-none lg:w-80 lg:border-l lg:border-t-0 lg:p-6">
          {inspector}
        </aside>
      </div>
    </div>
  );
}
