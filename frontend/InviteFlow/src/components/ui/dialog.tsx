import * as React from "react";
import * as DialogPrimitive from "@radix-ui/react-dialog";
import { X } from "lucide-react";
import { motion } from "motion/react";
import { OverlayOpenContext, useOverlayOpen } from "@/components/ui/overlay-open";
import { useReducedMotion } from "@/hooks/useReducedMotion";
import { uiSpring } from "@/lib/physics";
import { cn } from "@/lib/utils";

const DialogTrigger = DialogPrimitive.Trigger;
const DialogPortal = DialogPrimitive.Portal;
const DialogClose = DialogPrimitive.Close;

function Dialog({
  open,
  onOpenChange,
  children,
  ...props
}: React.ComponentProps<typeof DialogPrimitive.Root>) {
  return (
    <OverlayOpenContext.Provider value={{ open: !!open, onOpenChange: onOpenChange ?? (() => undefined) }}>
      <DialogPrimitive.Root open={open} onOpenChange={onOpenChange} {...props}>
        {children}
      </DialogPrimitive.Root>
    </OverlayOpenContext.Provider>
  );
}

function DialogOverlay({ className, ...props }: React.ComponentProps<typeof DialogPrimitive.Overlay>) {
  const { open } = useOverlayOpen();
  const reduced = useReducedMotion();
  return (
    <DialogPrimitive.Overlay asChild forceMount {...props}>
      <motion.div
        className={cn("scrim fixed inset-0 z-40", className)}
        initial={false}
        animate={{ opacity: open ? 1 : 0 }}
        transition={reduced ? { duration: 0.2, ease: "easeOut" } : uiSpring}
        style={{ pointerEvents: open ? "auto" : "none" }}
      />
    </DialogPrimitive.Overlay>
  );
}

function DialogContent({
  className,
  children,
  ...props
}: React.ComponentProps<typeof DialogPrimitive.Content>) {
  const { open } = useOverlayOpen();
  const reduced = useReducedMotion();
  const [present, setPresent] = React.useState(open);

  React.useEffect(() => {
    if (open) setPresent(true);
  }, [open]);

  if (!present) return null;

  return (
    <DialogPortal forceMount>
      <DialogOverlay />
      <DialogPrimitive.Content asChild forceMount {...props}>
        <motion.div
          className={cn(
            "sheet-material impression fixed left-1/2 top-1/2 z-40 grid w-[calc(100%-2rem)] max-w-lg gap-4 p-6 outline-none will-change-transform",
            className
          )}
          initial={reduced ? { opacity: 0, x: "-50%", y: "-50%" } : { opacity: 0, scale: 0.96, x: "-50%", y: "-50%" }}
          animate={
            reduced
              ? { opacity: open ? 1 : 0, x: "-50%", y: "-50%" }
              : { opacity: open ? 1 : 0, scale: open ? 1 : 0.96, x: "-50%", y: "-50%" }
          }
          transition={reduced ? { duration: 0.2, ease: "easeOut" } : uiSpring}
          onAnimationComplete={() => {
            if (!open) setPresent(false);
          }}
        >
          {children}
          <DialogPrimitive.Close className="press absolute right-4 top-4 inline-flex size-11 items-center justify-center rounded-md text-muted-foreground hover:bg-muted hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring">
            <X className="size-4" />
            <span className="sr-only">Close</span>
          </DialogPrimitive.Close>
        </motion.div>
      </DialogPrimitive.Content>
    </DialogPortal>
  );
}

function DialogHeader({ className, ...props }: React.ComponentProps<"div">) {
  return <div className={cn("flex flex-col gap-1 pr-8", className)} {...props} />;
}

function DialogFooter({ className, ...props }: React.ComponentProps<"div">) {
  return <div className={cn("flex flex-col-reverse gap-2 sm:flex-row sm:justify-end", className)} {...props} />;
}

function DialogTitle({ className, ...props }: React.ComponentProps<typeof DialogPrimitive.Title>) {
  return (
    <DialogPrimitive.Title className={cn("font-display text-xl font-semibold leading-none", className)} {...props} />
  );
}

function DialogDescription({ className, ...props }: React.ComponentProps<typeof DialogPrimitive.Description>) {
  return (
    <DialogPrimitive.Description className={cn("text-sm leading-6 text-muted-foreground", className)} {...props} />
  );
}

export {
  Dialog,
  DialogTrigger,
  DialogPortal,
  DialogClose,
  DialogOverlay,
  DialogContent,
  DialogHeader,
  DialogFooter,
  DialogTitle,
  DialogDescription,
};
