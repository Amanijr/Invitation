import * as React from "react";
import { cn } from "@/lib/utils";

function Input({ className, type, value, ...props }: React.ComponentProps<"input">) {
  const controlled = value !== undefined;
  return (
    <input
      type={type}
      data-slot="input"
      className={cn(
        "flex h-11 w-full rounded-md border border-input bg-card px-3 py-2 text-sm transition-colors duration-150 file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-destructive/30",
        className
      )}
      {...props}
      {...(controlled ? { value: value ?? "" } : {})}
    />
  );
}

export { Input };
