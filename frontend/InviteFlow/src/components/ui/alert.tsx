import * as React from "react";
import { cn } from "@/lib/utils";

function Alert({
  className,
  variant = "default",
  ...props
}: React.ComponentProps<"div"> & { variant?: "default" | "destructive" }) {
  return (
    <div
      role="alert"
      className={cn(
        "rounded-md border px-4 py-3 text-sm",
        variant === "destructive"
          ? "border-destructive/40 bg-destructive/10 text-destructive"
          : "border-border bg-card text-foreground",
        className
      )}
      {...props}
    />
  );
}

export { Alert };
