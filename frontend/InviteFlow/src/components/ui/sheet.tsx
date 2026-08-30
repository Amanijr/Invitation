import * as React from "react";
import * as DialogPrimitive from "@radix-ui/react-dialog";
import { X } from "lucide-react";
import { animate, motion, useDragControls, useMotionValue, type AnimationPlaybackControls, type PanInfo } from "motion/react";
import { OverlayOpenContext, useOverlayOpen } from "@/components/ui/overlay-open";
import { useReducedMotion } from "@/hooks/useReducedMotion";
import { flickSpring, project, sheetSpring, uiSpring } from "@/lib/physics";
import { cn } from "@/lib/utils";

const SheetTrigger = DialogPrimitive.Trigger;
const SheetClose = DialogPrimitive.Close;
const SheetPortal = DialogPrimitive.Portal;

function Sheet({
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

function SheetOverlay({ className, ...props }: React.ComponentProps<typeof DialogPrimitive.Overlay>) {
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

type Side = "right" | "left" | "bottom";

function dismissDistance(side: Side, el: HTMLElement | null) {
  if (!el) return side === "bottom" ? 640 : 420;
  const rect = el.getBoundingClientRect();
  return (side === "bottom" ? rect.height : rect.width) + 24;
}

function axisValue(side: Side, info: PanInfo) {
  return side === "bottom" ? { offset: info.offset.y, velocity: info.velocity.y } : { offset: info.offset.x, velocity: info.velocity.x };
}

function SheetContent({
  className,
  children,
  side = "right",
  ...props
}: React.ComponentProps<typeof DialogPrimitive.Content> & { side?: Side }) {
  const { open, onOpenChange } = useOverlayOpen();
  const reduced = useReducedMotion();
  const [present, setPresent] = React.useState(open);
  const panelRef = React.useRef<HTMLDivElement>(null);
  const x = useMotionValue(side === "bottom" ? 0 : side === "left" ? -480 : 480);
  const y = useMotionValue(side === "bottom" ? 720 : 0);
  const mv = side === "bottom" ? y : x;
  const dragControls = useDragControls();
  const controlsRef = React.useRef<AnimationPlaybackControls | null>(null);
  const gestureVelocity = React.useRef(0);
  const initialized = React.useRef(false);
  const openRef = React.useRef(open);
  openRef.current = open;

  React.useEffect(() => {
    if (open) setPresent(true);
  }, [open]);

  React.useLayoutEffect(() => {
    if (!present) {
      initialized.current = false;
      return;
    }
    if (reduced) {
      mv.set(0);
      if (!open) {
        const id = window.setTimeout(() => setPresent(false), 200);
        return () => window.clearTimeout(id);
      }
      return;
    }

    const closed = side === "left" ? -dismissDistance(side, panelRef.current) : dismissDistance(side, panelRef.current);
    if (!initialized.current) {
      mv.set(open ? closed : 0);
      initialized.current = true;
    }

    const target = open ? 0 : closed;
    const velocity = gestureVelocity.current;
    gestureVelocity.current = 0;
    const spring = Math.abs(velocity) > 400 ? flickSpring : sheetSpring;
    controlsRef.current?.stop();
    const animation = animate(mv, target, { ...spring, velocity });
    controlsRef.current = animation;

    let cancelled = false;
    const timeout = window.setTimeout(() => {
      if (!cancelled && !openRef.current) setPresent(false);
    }, 420);

    return () => {
      cancelled = true;
      window.clearTimeout(timeout);
    };
  }, [open, present, reduced, side, mv]);

  const onDragEnd = (_: MouseEvent | TouchEvent | PointerEvent, info: PanInfo) => {
    const rect = panelRef.current?.getBoundingClientRect();
    if (!rect) return;
    const { offset, velocity } = axisValue(side, info);
    const dim = side === "bottom" ? rect.height : rect.width;
    const projected = offset + project(velocity);
    const flick =
      side === "left" ? velocity < -500 : velocity > 500;
    const crossed =
      side === "left" ? projected < -dim * 0.25 : projected > dim * 0.25;
    gestureVelocity.current = velocity;
    if (flick || crossed) {
      onOpenChange(false);
      return;
    }
    const spring = Math.abs(velocity) > 400 ? flickSpring : sheetSpring;
    controlsRef.current?.stop();
    controlsRef.current = animate(mv, 0, { ...spring, velocity });
    gestureVelocity.current = 0;
  };

  if (!present) return null;

  const placement =
    side === "right"
      ? "inset-y-0 right-0 h-full w-full max-w-md"
      : side === "left"
        ? "inset-y-0 left-0 h-full w-full max-w-xs"
        : "inset-x-0 bottom-0 max-h-[90dvh]";

  return (
    <SheetPortal forceMount>
      <SheetOverlay />
      <DialogPrimitive.Content asChild forceMount {...props}>
        <motion.div
          ref={panelRef}
          className={cn(
            "sheet-material pointer-events-auto fixed z-40 flex min-h-0 flex-col overflow-hidden outline-none will-change-transform",
            placement,
            className
          )}
          initial={false}
          style={reduced ? undefined : { x, y, pointerEvents: open ? "auto" : "none" }}
          animate={reduced ? { opacity: open ? 1 : 0 } : undefined}
          transition={reduced ? { duration: 0.2, ease: "easeOut" } : undefined}
          drag={reduced ? false : side === "bottom" ? "y" : "x"}
          dragListener={false}
          dragControls={dragControls}
          dragConstraints={side === "bottom" ? { top: 0 } : side === "left" ? { right: 0 } : { left: 0 }}
          dragElastic={0.18}
          dragMomentum={false}
          onDragEnd={reduced ? undefined : onDragEnd}
          onPointerDown={(event) => {
            if (!open) {
              controlsRef.current?.stop();
              onOpenChange(true);
            }
            if (reduced) return;
            const target = event.target as HTMLElement;
            if (target.closest("button, input, textarea, select, a, label, [role='button']")) return;
            dragControls.start(event);
          }}
          onPointerDownCapture={() => {
            if (!open) {
              controlsRef.current?.stop();
              onOpenChange(true);
            }
          }}
        >
          {side === "bottom" ? (
            <div className="flex justify-center pt-3" aria-hidden>
              <div className="h-1.5 w-10 rounded-full bg-foreground/20" />
            </div>
          ) : (
            <div
              className={cn("absolute top-0 z-10 h-full w-3 cursor-grab", side === "right" ? "left-0" : "right-0")}
              aria-hidden
            />
          )}
          {children}
          <DialogPrimitive.Close className="press absolute right-4 top-4 inline-flex size-11 items-center justify-center rounded-md text-muted-foreground hover:bg-muted hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring">
            <X className="size-4" />
            <span className="sr-only">Close</span>
          </DialogPrimitive.Close>
        </motion.div>
      </DialogPrimitive.Content>
    </SheetPortal>
  );
}

function SheetHeader({ className, ...props }: React.ComponentProps<"div">) {
  return <div className={cn("flex flex-col gap-1 p-6 pr-14", className)} {...props} />;
}

function SheetFooter({ className, ...props }: React.ComponentProps<"div">) {
  return <div className={cn("mt-auto flex gap-2 p-6", className)} {...props} />;
}

function SheetTitle({ className, ...props }: React.ComponentProps<typeof DialogPrimitive.Title>) {
  return <DialogPrimitive.Title className={cn("font-display text-xl font-semibold", className)} {...props} />;
}

function SheetDescription({ className, ...props }: React.ComponentProps<typeof DialogPrimitive.Description>) {
  return (
    <DialogPrimitive.Description className={cn("text-sm leading-6 text-muted-foreground", className)} {...props} />
  );
}

export {
  Sheet,
  SheetTrigger,
  SheetClose,
  SheetPortal,
  SheetOverlay,
  SheetContent,
  SheetHeader,
  SheetFooter,
  SheetTitle,
  SheetDescription,
};
