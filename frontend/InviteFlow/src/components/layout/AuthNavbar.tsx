import { FC } from "react";
import { AuthMode } from "../../types/auth.types";

interface AuthNavbarProps {
  mode: AuthMode;
  onToggleMode: () => void;
}

const NAV_LINKS: string[] = ["Features", "Pricing", "About"];

const AuthNavbar: FC<AuthNavbarProps> = ({ mode, onToggleMode }) => (
  <header className="flex items-center justify-between px-10 py-5 border-b border-zinc-800/60">
    {/* Logo */}
    <div className="flex items-center gap-3">
      <div
        className="w-8 h-8 flex items-center justify-center"
        style={{
          background: "linear-gradient(135deg, #06b6d4, #3b82f6)",
          boxShadow: "0 0 14px rgba(6,182,212,0.4)",
        }}
      >
        <svg viewBox="0 0 24 24" fill="none" className="w-4 h-4">
          <path
            d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
            stroke="white"
            strokeWidth="1.8"
            strokeLinecap="round"
          />
        </svg>
      </div>
      <span
        className="text-sm font-bold uppercase text-zinc-100"
        style={{ letterSpacing: "0.18em" }}
      >
        InviteFlow
      </span>
      <span
        className="text-cyan-400 border border-cyan-400/30 px-2 py-0.5 rounded-full font-medium"
        style={{ fontSize: "10px" }}
      >
        BETA
      </span>
    </div>

    {/* Nav links */}
    <nav className="hidden md:flex items-center gap-8">
      {NAV_LINKS.map((item) => (
        <a
          key={item}
          href="#"
          className="text-xs tracking-widest uppercase text-zinc-500 hover:text-zinc-200 transition-colors font-medium"
        >
          {item}
        </a>
      ))}
    </nav>

    {/* Mode toggle */}
    <button
      onClick={onToggleMode}
      className="text-xs tracking-widest uppercase font-semibold text-zinc-300 border border-zinc-700 px-5 py-2 hover:border-cyan-400 hover:text-cyan-400 transition-all duration-200"
    >
      {mode === "login" ? "Register" : "Sign In"}
    </button>
  </header>
);

export default AuthNavbar;