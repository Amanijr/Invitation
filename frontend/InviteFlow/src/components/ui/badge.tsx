import * as React from "react";
import { cn } from "@/lib/utils";

function Badge({
  className,
  variant = "default",
  ...props
}: React.ComponentProps<"span"> & { variant?: "default" | "secondary" | "outline" | "success" | "warning" | "destructive" }) {
  return (
    <span
      data-slot="badge"
      className={cn(
        "inline-flex items-center rounded-md border px-2 py-1 text-xs font-medium",
        variant === "default" && "border-transparent bg-accent text-accent-foreground",
        variant === "secondary" && "border-transparent bg-secondary text-secondary-foreground",
        variant === "outline" && "border-border text-foreground",
        variant === "success" && "border-transparent bg-success/15 text-success",
        variant === "warning" && "border-transparent bg-warning/15 text-warning",
        variant === "destructive" && "border-transparent bg-destructive/15 text-destructive",
        className
      )}
      {...props}
    />
  );
}

export { Badge };
