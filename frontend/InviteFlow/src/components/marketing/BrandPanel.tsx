import { FC } from "react";

const STATS: { value: string; label: string }[] = [
  { value: "12K+", label: "Invitations Sent" },
  { value: "98%",  label: "Delivery Rate" },
  { value: "340+", label: "Events Managed" },
];

const FEATURES: string[] = ["QR Codes", "Bulk Upload", "PDF Receipts", "Email Delivery"];

const BrandPanel: FC = () => (
  <div className="relative bg-zinc-900 border border-zinc-800 px-10 py-14 flex flex-col justify-between overflow-hidden">
    {/* Top edge glow */}
    <div
      className="absolute top-0 left-0 right-0 h-px"
      style={{ background: "linear-gradient(90deg, transparent, #06b6d4, transparent)" }}
    />
    {/* Inner glow blob */}
    <div
      className="absolute -top-24 -right-24 w-64 h-64 rounded-full"
      style={{ background: "radial-gradient(circle, #06b6d4, transparent 70%)", opacity: 0.1 }}
    />

    {/* Content */}
    <div className="relative z-10">
      <div className="flex items-center gap-2 mb-8">
        <span className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse" />
        <span className="text-xs font-medium tracking-widest uppercase text-cyan-400">
          Platform Online
        </span>
      </div>

      <p className="text-xs font-medium tracking-widest uppercase text-zinc-500 mb-3">
        Invitation Management
      </p>

      <h1 className="text-4xl font-light leading-tight text-zinc-100 mb-6">
        Orchestrate every<br />
        <span
          className="font-normal italic text-transparent bg-clip-text"
          style={{ backgroundImage: "linear-gradient(90deg, #06b6d4, #3b82f6)" }}
        >
          invitation,
        </span>
        <br />
        flawlessly.
      </h1>

      <p className="text-zinc-500 text-sm leading-relaxed max-w-xs">
        From intimate gatherings to large-scale corporate events — manage, send, and track
        invitations with precision.
      </p>
    </div>

    {/* Stats */}
    <div className="relative z-10 mt-12 grid grid-cols-3 gap-4 border-t border-zinc-800 pt-8">
      {STATS.map((s) => (
        <div key={s.label}>
          <p
            className="text-2xl font-light text-transparent bg-clip-text mb-1"
            style={{ backgroundImage: "linear-gradient(135deg, #06b6d4, #3b82f6)" }}
          >
            {s.value}
          </p>
          <p className="text-xs text-zinc-600 leading-tight">{s.label}</p>
        </div>
      ))}
    </div>

    {/* Feature tags */}
    <div className="relative z-10 mt-8 flex flex-wrap gap-2">
      {FEATURES.map((tag) => (
        <span
          key={tag}
          className="text-xs border border-zinc-700 text-zinc-500 px-3 py-1 tracking-wide hover:border-cyan-400/50 hover:text-cyan-400 transition-all duration-200 cursor-default"
        >
          {tag}
        </span>
      ))}
    </div>

    {/* Bottom edge glow */}
    <div
      className="absolute bottom-0 left-0 right-0 h-px"
      style={{ background: "linear-gradient(90deg, transparent, #3b82f6, transparent)" }}
    />
  </div>
);

export default BrandPanel;