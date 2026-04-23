import { FC, useState, useEffect } from "react";
import { Phase } from "../../types/auth.types";

interface SplashScreenProps {
  onDone: () => void;
}

const SplashScreen: FC<SplashScreenProps> = ({ onDone }) => {
  const [phase, setPhase] = useState<Phase>("enter");

  useEffect(() => {
    const t1 = setTimeout(() => setPhase("hold"), 800);
    const t2 = setTimeout(() => setPhase("exit"), 1900);
    const t3 = setTimeout(() => onDone(), 2600);
    return () => { clearTimeout(t1); clearTimeout(t2); clearTimeout(t3); };
  }, [onDone]);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-950"
      style={{
        opacity: phase === "exit" ? 0 : 1,
        transform: phase === "exit" ? "scale(1.04)" : "scale(1)",
        transition: "opacity 0.7s ease-out, transform 0.7s ease-out",
        pointerEvents: phase === "exit" ? "none" : "all",
      }}
    >
      {/* Ambient glow blobs */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div
          className="absolute top-1/2 left-1/2 w-[520px] h-[520px] rounded-full"
          style={{
            background: "radial-gradient(circle, #06b6d4, transparent 70%)",
            opacity: 0.15,
            transform: `translate(-50%, -50%) scale(${phase === "hold" ? 1.5 : 0.7})`,
            transition: "transform 1.2s ease-out",
          }}
        />
        <div
          className="absolute top-1/2 left-1/2 w-80 h-80 rounded-full"
          style={{
            background: "radial-gradient(circle, #3b82f6, transparent 70%)",
            opacity: 0.1,
            transform: `translate(-20%, -65%) scale(${phase === "hold" ? 1.3 : 0.6})`,
            transition: "transform 1.4s ease-out",
          }}
        />
      </div>

      {/* Subtle grid */}
      <div
        className="absolute inset-0 pointer-events-none"
        style={{
          backgroundImage:
            "linear-gradient(#06b6d4 1px, transparent 1px), linear-gradient(90deg, #06b6d4 1px, transparent 1px)",
          backgroundSize: "60px 60px",
          opacity: 0.03,
        }}
      />

      {/* Logo lockup */}
      <div className="relative flex flex-col items-center">
        {/* Icon box */}
        <div
          className="relative w-24 h-24 flex items-center justify-center mb-7"
          style={{
            background: "linear-gradient(135deg, #06b6d4, #3b82f6)",
            boxShadow:
              phase === "hold"
                ? "0 0 80px rgba(6,182,212,0.55), 0 0 160px rgba(6,182,212,0.18)"
                : "0 0 20px rgba(6,182,212,0.25)",
            opacity: phase === "enter" ? 0 : 1,
            transform: phase === "enter" ? "scale(0.7) translateY(14px)" : "scale(1) translateY(0)",
            transition:
              "opacity 0.5s ease-out, transform 0.55s cubic-bezier(0.34,1.56,0.64,1), box-shadow 1s ease-out",
          }}
        >
          <svg viewBox="0 0 24 24" fill="none" className="w-12 h-12">
            <path
              d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
              stroke="white"
              strokeWidth="1.5"
              strokeLinecap="round"
            />
          </svg>
          <span className="absolute top-0 left-0 w-3 h-3 border-t-2 border-l-2 border-white/40" />
          <span className="absolute top-0 right-0 w-3 h-3 border-t-2 border-r-2 border-white/40" />
          <span className="absolute bottom-0 left-0 w-3 h-3 border-b-2 border-l-2 border-white/40" />
          <span className="absolute bottom-0 right-0 w-3 h-3 border-b-2 border-r-2 border-white/40" />
        </div>

        {phase === "hold" && (
          <div className="absolute top-0 w-24 h-24 border border-cyan-400/30 animate-ping" />
        )}

        {/* Wordmark */}
        <div style={{ overflow: "hidden" }}>
          <p
            className="text-3xl font-bold uppercase text-zinc-100"
            style={{
              letterSpacing: "0.3em",
              opacity: phase === "enter" ? 0 : 1,
              transform: phase === "enter" ? "translateY(110%)" : "translateY(0)",
              transition: "opacity 0.55s ease-out 0.18s, transform 0.55s ease-out 0.18s",
            }}
          >
            InviteFlow
          </p>
        </div>

        {/* Tagline */}
        <div style={{ overflow: "hidden", marginTop: "8px" }}>
          <p
            className="text-xs font-medium uppercase"
            style={{
              letterSpacing: "0.25em",
              color: "rgba(6,182,212,0.7)",
              opacity: phase === "enter" ? 0 : 1,
              transform: phase === "enter" ? "translateY(110%)" : "translateY(0)",
              transition: "opacity 0.5s ease-out 0.32s, transform 0.5s ease-out 0.32s",
            }}
          >
            Invitation Management Platform
          </p>
        </div>

        {/* Loading bar */}
        <div
          className="mt-10 rounded-full overflow-hidden"
          style={{ width: "160px", height: "2px", background: "#27272a" }}
        >
          <div
            style={{
              height: "100%",
              borderRadius: "9999px",
              background: "linear-gradient(90deg, #06b6d4, #3b82f6)",
              boxShadow: "0 0 10px rgba(6,182,212,0.7)",
              width: phase === "enter" ? "0%" : phase === "hold" ? "65%" : "100%",
              transition:
                phase === "enter"
                  ? "width 0.9s ease-out"
                  : phase === "hold"
                  ? "width 1s ease-out"
                  : "width 0.45s ease-in",
            }}
          />
        </div>

        <p
          className="text-xs text-zinc-700 tracking-widest mt-4"
          style={{ opacity: phase === "enter" ? 0 : 0.8, transition: "opacity 0.5s ease-out 0.45s" }}
        >
          v1.0.0 · Loading…
        </p>
      </div>
    </div>
  );
};

export default SplashScreen;