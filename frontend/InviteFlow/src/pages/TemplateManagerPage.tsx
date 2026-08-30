import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { FileText, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { TemplateUploadModal } from "@/components/templates/TemplateUploadModal";
import { PageHeader } from "@/components/layout/PageHeader";
import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { NativeSelect } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { apiFetch, readError, readJson, templateCoverSrc } from "@/lib/api";
import { getPackage, getTemplate, occasionToEventType, EVENT_TYPES, type Occasion } from "@/lib/catalog";
import { getIntent, getActiveEventId } from "@/lib/session";

export interface TemplateResponseDto {
  id: string;
  eventId: string;
  eventType: string;
  templateName: string;
  originalFileName?: string;
  mimeType?: string;
  fileSize?: number;
  fileUrl?: string;
  storagePath?: string;
  previewImageUrl?: string;
  width?: number;
  height?: number;
  active: boolean;
  createdAt?: string;
}

export function TemplateManagerPage() {
  const [templates, setTemplates] = useState<TemplateResponseDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const intent = getIntent();
  const heldTemplate = intent ? getTemplate(intent.templateId) : undefined;
  const heldPackage = intent ? getPackage(intent.packageId) : undefined;
  const heldName = heldTemplate?.name ?? intent?.templateName;
  const heldType = intent?.occasion
    ? occasionToEventType(intent.occasion as Occasion)
    : heldTemplate
      ? occasionToEventType(heldTemplate.occasion)
      : "ALL";
  const [selectedEventType, setSelectedEventType] = useState(heldType);
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [previewTemplate, setPreviewTemplate] = useState<TemplateResponseDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<TemplateResponseDto | null>(null);
  const [clearAllOpen, setClearAllOpen] = useState(false);
  const [clearingAll, setClearingAll] = useState(false);

  const fetchTemplates = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiFetch("/templates");
      if (!res.ok) throw new Error(await readError(res));
      setTemplates(await readJson<TemplateResponseDto[]>(res));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load templates.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTemplates();
  }, []);

  const handleUseForEvent = async (template: TemplateResponseDto) => {
    const eventId = getActiveEventId();
    if (!eventId) {
      setError("Open Guests or Analytics and select an event first.");
      return;
    }
    try {
      const res = await apiFetch(`/events/${eventId}/template`, {
        method: "PUT",
        body: JSON.stringify({ templateId: template.id, scope: "NEW_GUESTS_ONLY" }),
      });
      if (!res.ok) throw new Error(await readError(res));
      toast.success(`“${template.templateName}” is now this event’s card. Existing invitations were left unchanged.`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not assign this card to the event.");
    }
  };

  const handleToggleActive = async (template: TemplateResponseDto) => {
    const endpoint = template.active ? "deactivate" : "activate";
    try {
      const res = await apiFetch(`/templates/${template.id}/${endpoint}`, {
        method: "PATCH",
      });
      if (!res.ok) throw new Error(await readError(res));
      fetchTemplates();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not update status.");
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      const res = await apiFetch(`/templates/${deleteTarget.id}`, { method: "DELETE" });
      if (!res.ok) throw new Error(await readError(res));
      setDeleteTarget(null);
      fetchTemplates();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not delete template.");
    }
  };

  const filteredTemplates = templates.filter((t) => {
    const matchesSearch = t.templateName.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesType = selectedEventType === "ALL" || t.eventType === selectedEventType;
    return matchesSearch && matchesType;
  });

  const handleDeleteAll = async () => {
    setClearingAll(true);
    setError(null);
    try {
      const failures: string[] = [];
      for (const template of templates) {
        const res = await apiFetch(`/templates/${template.id}`, { method: "DELETE" });
        if (!res.ok) failures.push(template.templateName);
      }
      setClearAllOpen(false);
      await fetchTemplates();
      if (failures.length) {
        setError(`Could not remove ${failures.length} template${failures.length === 1 ? "" : "s"}.`);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not remove templates.");
    } finally {
      setClearingAll(false);
    }
  };

  const formatFileSize = (bytes?: number) => {
    if (!bytes) return "—";
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div>
      <PageHeader title="Templates" description="Upload your finished card. The press adds each guest’s name and QR.">
        {templates.length > 0 ? (
          <Button variant="outline" onClick={() => setClearAllOpen(true)}>
            Remove all
          </Button>
        ) : null}
        <Button onClick={() => setIsUploadOpen(true)}>Upload template</Button>
      </PageHeader>

      {heldName && heldPackage ? (
        <Alert className="mb-6">
          Holding {heldName} on {heldPackage.name}. Upload a matching background or continue from an existing card.
        </Alert>
      ) : null}
      {error ? (
        <Alert variant="destructive" className="mb-6">
          {error}
        </Alert>
      ) : null}

      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center">
        <Input
          placeholder="Search templates"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="max-w-md"
        />
        <NativeSelect
          value={selectedEventType}
          onChange={(e) => setSelectedEventType(e.target.value)}
          className="w-auto sm:ml-auto"
        >
          <option value="ALL">All categories</option>
          {EVENT_TYPES.map((type) => (
            <option key={type.id} value={type.id}>
              {type.label}
            </option>
          ))}
        </NativeSelect>
      </div>

      {loading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-64" />
          ))}
        </div>
      ) : templates.length === 0 ? (
        <Card className="p-12 text-center">
          <p className="font-medium">No templates yet</p>
          <p className="mt-2 text-sm text-muted-foreground">Upload your finished card, then place the name and QR.</p>
          <Button className="mt-6" onClick={() => setIsUploadOpen(true)}>
            Upload first template
          </Button>
        </Card>
      ) : filteredTemplates.length === 0 ? (
        <Card className="p-12 text-center">
          <p className="font-medium">No templates match</p>
          <p className="mt-2 text-sm text-muted-foreground">Try another search or category.</p>
        </Card>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filteredTemplates.map((template) => (
            <Card key={template.id} className="overflow-hidden p-0">
              <div className="relative aspect-[16/10] bg-muted">
                {(() => {
                  const cover = templateCoverSrc(template);
                  if (!cover || template.mimeType?.includes("pdf")) {
                    return (
                      <div className="flex h-full flex-col items-center justify-center gap-2 text-muted-foreground">
                        <FileText className="size-8" />
                        <span className="text-xs">{template.mimeType?.includes("pdf") ? "PDF" : "No card file"}</span>
                      </div>
                    );
                  }
                  return (
                    <img
                      src={cover}
                      alt={template.templateName}
                      className="h-full w-full object-cover"
                      onError={(e) => {
                        (e.target as HTMLElement).style.display = "none";
                      }}
                    />
                  );
                })()}
                <button
                  type="button"
                  onClick={() => handleToggleActive(template)}
                  className="absolute left-3 top-3"
                >
                  <Badge variant={template.active ? "success" : "warning"}>
                    {template.active ? "Active" : "Inactive"}
                  </Badge>
                </button>
              </div>
              <CardHeader>
                <p className="text-xs uppercase tracking-wide text-muted-foreground">{template.eventType}</p>
                <CardTitle className="text-base">{template.templateName}</CardTitle>
                <p className="font-mono text-xs text-muted-foreground">
                  {template.width}×{template.height} · {formatFileSize(template.fileSize)}
                </p>
              </CardHeader>
              <CardContent className="pt-0">
                <p className="truncate font-mono text-xs text-muted-foreground">
                  {template.originalFileName || "design.png"}
                </p>
              </CardContent>
              <CardFooter className="gap-2">
                <Button asChild className="flex-1">
                  <Link to={`/templates/${template.id}/designer`}>Place fields</Link>
                </Button>
                <Button variant="outline" onClick={() => void handleUseForEvent(template)}>
                  Use for event
                </Button>
                <Button variant="outline" onClick={() => setPreviewTemplate(template)}>
                  Preview
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label="Delete template"
                  onClick={() => setDeleteTarget(template)}
                >
                  <Trash2 className="size-4" />
                </Button>
              </CardFooter>
            </Card>
          ))}
        </div>
      )}

      <TemplateUploadModal isOpen={isUploadOpen} onClose={() => setIsUploadOpen(false)} onUploadSuccess={fetchTemplates} />

      <Dialog open={!!previewTemplate} onOpenChange={(open) => !open && setPreviewTemplate(null)}>
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle>{previewTemplate?.templateName}</DialogTitle>
            <DialogDescription>
              {previewTemplate?.eventType} · {previewTemplate?.width}×{previewTemplate?.height}px
            </DialogDescription>
          </DialogHeader>
          {previewTemplate ? (
            <div className="flex items-center justify-center rounded-md border border-border bg-muted p-4">
              {templateCoverSrc(previewTemplate) && !previewTemplate.mimeType?.includes("pdf") ? (
                <img
                  src={templateCoverSrc(previewTemplate)}
                  alt={previewTemplate.templateName}
                  className="max-h-[65vh] object-contain"
                />
              ) : (
                <div className="flex flex-col items-center gap-2 py-12 text-muted-foreground">
                  <FileText className="size-10" />
                  <p className="text-sm">This template has no card image on file.</p>
                </div>
              )}
            </div>
          ) : null}
        </DialogContent>
      </Dialog>

      <Dialog open={!!deleteTarget} onOpenChange={(open) => !open && setDeleteTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete template?</DialogTitle>
            <DialogDescription>
              {deleteTarget ? `“${deleteTarget.templateName}” will be removed.` : null}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteTarget(null)}>
              Keep template
            </Button>
            <Button variant="destructive" onClick={handleDelete}>
              Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      <Dialog open={clearAllOpen} onOpenChange={setClearAllOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Remove every template?</DialogTitle>
            <DialogDescription>
              All {templates.length} card backgrounds will be deleted. Upload your own artwork after this.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setClearAllOpen(false)}>
              Keep them
            </Button>
            <Button variant="destructive" disabled={clearingAll} onClick={() => void handleDeleteAll()}>
              {clearingAll ? "Removing…" : "Remove all"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
