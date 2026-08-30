import { type FormEvent, useState } from "react";
import { Alert } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { NativeSelect } from "@/components/ui/select";
import { EVENT_TYPES, getTemplate, occasionToEventType } from "@/lib/catalog";
import { apiFetch, readError } from "@/lib/api";
import { getActiveEventId, getIntent } from "@/lib/session";

interface TemplateUploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUploadSuccess: () => void;
}

export function TemplateUploadModal({ isOpen, onClose, onUploadSuccess }: TemplateUploadModalProps) {
  const held = (() => {
    const intent = getIntent();
    return intent ? getTemplate(intent.templateId) : undefined;
  })();
  const [templateName, setTemplateName] = useState(held?.name ?? "");
  const [eventType, setEventType] = useState<string>(held ? occasionToEventType(held.occasion) : "WEDDING");
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isDragOver, setIsDragOver] = useState(false);

  const handleFileChange = (selectedFile: File | null) => {
    setError(null);
    if (!selectedFile) {
      setFile(null);
      setPreviewUrl(null);
      return;
    }
    const validTypes = ["image/png", "image/jpeg", "image/jpg", "application/pdf"];
    const ext = selectedFile.name.split(".").pop()?.toLowerCase();
    const validExts = ["png", "jpg", "jpeg", "pdf"];
    if (!validTypes.includes(selectedFile.type) && (!ext || !validExts.includes(ext))) {
      setError("Use a PNG, JPEG, or PDF file.");
      return;
    }
    if (selectedFile.size > 10 * 1024 * 1024) {
      setError("File is larger than 10MB.");
      return;
    }
    setFile(selectedFile);
    setPreviewUrl(selectedFile.type.startsWith("image/") ? URL.createObjectURL(selectedFile) : null);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!templateName.trim()) {
      setError("Name is required.");
      return;
    }
    if (!file) {
      setError("Choose a design file.");
      return;
    }
    setIsSubmitting(true);
    setError(null);
    const formData = new FormData();
    formData.append("templateName", templateName.trim());
    formData.append("eventType", eventType);
    formData.append("eventId", getActiveEventId() ?? "00000000-0000-0000-0000-000000000000");
    formData.append("file", file);
    try {
      const res = await apiFetch("/templates", { method: "POST", body: formData });
      if (!res.ok) throw new Error(await readError(res));
      setTemplateName("");
      setFile(null);
      setPreviewUrl(null);
      onUploadSuccess();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Upload failed.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Sheet open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <SheetContent>
        <form onSubmit={handleSubmit} className="flex h-full flex-col">
          <SheetHeader>
            <SheetTitle>Upload template</SheetTitle>
            <SheetDescription>PNG, JPEG, or PDF, up to 10MB.</SheetDescription>
          </SheetHeader>
          <div className="flex-1 space-y-4 overflow-y-auto px-6 py-4">
          {error ? (
            <Alert variant="destructive">
              {error}
            </Alert>
          ) : null}
          <div className="grid gap-4">
            <div className="space-y-2">
              <Label htmlFor="tpl-name">Name</Label>
              <Input
                id="tpl-name"
                required
                placeholder="Gold wedding card"
                value={templateName}
                onChange={(e) => setTemplateName(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="tpl-type">Category</Label>
              <NativeSelect id="tpl-type" value={eventType} onChange={(e) => setEventType(e.target.value)}>
                {EVENT_TYPES.map((type) => (
                  <option key={type.id} value={type.id}>
                    {type.label}
                  </option>
                ))}
              </NativeSelect>
            </div>
            <div
              onDragOver={(e) => {
                e.preventDefault();
                setIsDragOver(true);
              }}
              onDragLeave={() => setIsDragOver(false)}
              onDrop={(e) => {
                e.preventDefault();
                setIsDragOver(false);
                if (e.dataTransfer.files[0]) handleFileChange(e.dataTransfer.files[0]);
              }}
              className={`relative rounded-md border border-dashed px-6 py-8 text-center ${
                isDragOver ? "border-accent bg-muted" : "border-input"
              }`}
            >
              <input
                type="file"
                accept="image/png,image/jpeg,image/jpg,application/pdf"
                className="absolute inset-0 cursor-pointer opacity-0"
                onChange={(e) => handleFileChange(e.target.files?.[0] || null)}
              />
              {previewUrl ? (
                <img src={previewUrl} alt="" className="mx-auto h-24 rounded-md object-contain" />
              ) : (
                <p className="text-sm text-muted-foreground">{file ? file.name : "Drop a file or click to browse"}</p>
              )}
            </div>
          </div>
          </div>
          <SheetFooter>
            <Button type="button" variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" loading={isSubmitting}>
              Upload
            </Button>
          </SheetFooter>
        </form>
      </SheetContent>
    </Sheet>
  );
}
