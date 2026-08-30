import { cn } from "@/lib/utils";

export function PersonAvatar({
  size = "md",
  className,
}: {
  size?: "sm" | "md" | "lg";
  className?: string;
}) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        "relative flex shrink-0 items-end justify-center overflow-hidden rounded-full border bg-[#111318] text-[#F8F5EF]",
        size === "sm" && "size-8 border-sidebar-border",
        size === "md" && "size-12 border-border",
        size === "lg" && "size-28 border-[#C9A227]/70",
        className
      )}
    >
      <svg viewBox="0 0 80 80" className={cn("w-[78%]", size === "sm" ? "mb-[-6%]" : "mb-[-4%]")}>
        <circle cx="40" cy="26" r="16" fill="currentColor" />
        <path
          d="M8 80c2.4-18.8 14.8-32 32-32s29.6 13.2 32 32"
          fill="currentColor"
        />
      </svg>
    </span>
  );
}
