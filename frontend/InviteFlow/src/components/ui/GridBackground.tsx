import { FC } from "react";

const GridBackground: FC = () => (
  <div className="absolute inset-0 overflow-hidden pointer-events-none">
    {/* Dot grid */}
    <div
      className="absolute inset-0"
      style={{
        backgroundImage:
          "linear-gradient(#06b6d4 1px, transparent 1px), linear-gradient(90deg, #06b6d4 1px, transparent 1px)",
        backgroundSize: "60px 60px",
        opacity: 0.04,
      }}
    />
    {/* Top-left glow */}
    <div
      className="absolute -top-40 -left-40 w-96 h-96 rounded-full"
      style={{ background: "radial-gradient(circle, #06b6d4, transparent 70%)", opacity: 0.1 }}
    />
    {/* Bottom-right glow */}
    <div
      className="absolute -bottom-40 -right-20 w-80 h-80 rounded-full"
      style={{ background: "radial-gradient(circle, #3b82f6, transparent 70%)", opacity: 0.1 }}
    />
    {/* Horizontal scan line */}
    <div
      className="absolute left-0 right-0 h-px"
      style={{
        background: "linear-gradient(90deg, transparent, #06b6d4, transparent)",
        top: "35%",
        opacity: 0.2,
      }}
    />
  </div>
);

export default GridBackground;