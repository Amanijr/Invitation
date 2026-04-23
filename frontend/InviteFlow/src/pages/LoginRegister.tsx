import { useState, useEffect, FC } from "react";

/* ─── Types ──────────────────────────────────────────────────────── */
type Phase = "enter" | "hold" | "exit";
type Mode = "login" | "register";
type UserRole = "EVENT_MANAGER" | "GUEST";

interface FieldGroupProps {
  label: string;
  type?: string;
  placeholder: string;
  value: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  error?: string;
}

interface GlowButtonProps {
  children: React.ReactNode;
  loading: boolean;
  onClick: () => void;
}

interface SuccessStateProps {
  title: string;
  subtitle: string;
  action?: string;
  onAction?: () => void;
}

interface SplashScreenProps {
  onDone: () => void;
}

interface FormProps {
  onSwitch: () => void;
}

interface RegisterFormState {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirm: string;
  role: UserRole;
}

/* ─── Splash Screen ──────────────────────────────────────────────── */
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

/* ─── Shared helpers ─────────────────────────────────────────────── */
const INPUT_BASE =
  "w-full bg-zinc-900 border border-zinc-700 focus:border-cyan-400 outline-none px-4 py-3 text-zinc-100 placeholder-zinc-600 text-sm transition-all duration-200 rounded-sm";

const LABEL = "block text-xs font-medium tracking-widest uppercase text-zinc-500 mb-2";

const FieldGroup: FC<FieldGroupProps> = ({ label, type = "text", placeholder, value, onChange, error }) => (
  <div className="mb-5">
    <label className={LABEL}>{label}</label>
    <input
      type={type}
      placeholder={placeholder}
      value={value}
      onChange={onChange}
      className={INPUT_BASE + (error ? " border-red-500 focus:border-red-400" : "")}
      autoComplete="off"
    />
    {error && (
      <p className="text-xs text-red-400 mt-1.5 flex items-center gap-1">
        <span className="inline-block w-1 h-1 bg-red-400 rounded-full" />
        {error}
      </p>
    )}
  </div>
);

const GlowButton: FC<GlowButtonProps> = ({ children, loading, onClick }) => (
  <button
    onClick={onClick}
    disabled={loading}
    className="relative w-full py-3.5 text-xs font-bold tracking-widest uppercase transition-all duration-200 disabled:opacity-50 overflow-hidden group rounded-sm"
    style={{
      background: "linear-gradient(135deg, #06b6d4, #3b82f6)",
      boxShadow: loading ? "none" : "0 0 20px rgba(6,182,212,0.35)",
    }}
  >
    <span className="absolute inset-0 bg-white opacity-0 group-hover:opacity-10 transition-opacity duration-200" />
    {loading ? (
      <span className="flex items-center justify-center gap-2 text-white">
        <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
        </svg>
        Processing…
      </span>
    ) : (
      <span className="text-white">{children}</span>
    )}
  </button>
);

const SuccessState: FC<SuccessStateProps> = ({ title, subtitle, action, onAction }) => (
  <div className="flex flex-col items-center justify-center text-center px-8 py-20">
    <div className="relative mb-8">
      <div
        className="w-16 h-16 rounded-full flex items-center justify-center"
        style={{
          background: "linear-gradient(135deg, #06b6d4, #3b82f6)",
          boxShadow: "0 0 30px rgba(6,182,212,0.4)",
        }}
      >
        <svg className="w-7 h-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
        </svg>
      </div>
      <div
        className="absolute inset-0 rounded-full animate-ping opacity-20"
        style={{ background: "linear-gradient(135deg, #06b6d4, #3b82f6)" }}
      />
    </div>
    <p className="text-xs font-bold tracking-widest uppercase text-cyan-400 mb-2">{title}</p>
    <p className="text-2xl font-light text-zinc-100 mb-6">{subtitle}</p>
    {action && onAction && (
      <button
        onClick={onAction}
        className="text-xs tracking-widest uppercase font-semibold text-zinc-400 hover:text-cyan-400 transition-colors border border-zinc-700 hover:border-cyan-400 px-6 py-2.5"
      >
        {action}
      </button>
    )}
  </div>
);

/* ─── Login Form ─────────────────────────────────────────────────── */
const LoginForm: FC<FormProps> = ({ onSwitch }) => {
  const [email, setEmail] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState<boolean>(false);
  const [success, setSuccess] = useState<boolean>(false);

  const validate = (): Record<string, string> => {
    const e: Record<string, string> = {};
    if (!email.includes("@")) e.email = "Enter a valid email address";
    if (password.length < 6) e.password = "Password must be at least 6 characters";
    return e;
  };

  const handleSubmit = (): void => {
    const e = validate();
    if (Object.keys(e).length) { setErrors(e); return; }
    setErrors({});
    setLoading(true);
    setTimeout(() => { setLoading(false); setSuccess(true); }, 1600);
  };

  if (success) return <SuccessState title="Authenticated" subtitle="Welcome back." />;

  return (
    <div className="px-10 py-12 w-full">
      <div className="mb-10">
        <p className="text-xs font-medium tracking-widest uppercase text-cyan-400 mb-2">Welcome back</p>
        <h2 className="text-3xl font-light text-zinc-100 leading-tight">
          Sign in to your{" "}
          <span
            className="text-transparent bg-clip-text"
            style={{ backgroundImage: "linear-gradient(90deg, #06b6d4, #3b82f6)" }}
          >
            workspace
          </span>
        </h2>
      </div>

      <FieldGroup label="Email Address" type="email" placeholder="you@company.com"
        value={email} onChange={(e) => setEmail(e.target.value)} error={errors.email} />
      <FieldGroup label="Password" type="password" placeholder="••••••••"
        value={password} onChange={(e) => setPassword(e.target.value)} error={errors.password} />

      <div className="flex justify-end mb-8">
        <button className="text-xs text-zinc-500 hover:text-cyan-400 tracking-wide transition-colors">
          Forgot password?
        </button>
      </div>

      <GlowButton loading={loading} onClick={handleSubmit}>Sign In</GlowButton>

      <p className="text-center text-xs text-zinc-600 mt-6">
        No account?{" "}
        <button onClick={onSwitch} className="text-cyan-400 font-semibold hover:text-cyan-300 transition-colors">
          Create one
        </button>
      </p>
    </div>
  );
};

/* ─── Register Form ──────────────────────────────────────────────── */
const RegisterForm: FC<FormProps> = ({ onSwitch }) => {
  const [form, setForm] = useState<RegisterFormState>({
    firstName: "", lastName: "", email: "",
    password: "", confirm: "", role: "EVENT_MANAGER",
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState<boolean>(false);
  const [success, setSuccess] = useState<boolean>(false);

  const set =
    (field: keyof RegisterFormState) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>): void => {
      setForm((f) => ({ ...f, [field]: e.target.value }));
    };

  const validate = (): Record<string, string> => {
    const e: Record<string, string> = {};
    if (!form.firstName.trim()) e.firstName = "Required";
    if (!form.lastName.trim()) e.lastName = "Required";
    if (!form.email.includes("@")) e.email = "Enter a valid email";
    if (form.password.length < 6) e.password = "Min 6 characters";
    if (form.password !== form.confirm) e.confirm = "Passwords do not match";
    return e;
  };

  const handleSubmit = (): void => {
    const e = validate();
    if (Object.keys(e).length) { setErrors(e); return; }
    setErrors({});
    setLoading(true);
    setTimeout(() => { setLoading(false); setSuccess(true); }, 1800);
  };

  if (success) return (
    <SuccessState title="Account Created" subtitle="You're all set." action="Sign In →" onAction={onSwitch} />
  );

  return (
    <div className="px-10 py-10 w-full overflow-y-auto">
      <div className="mb-8">
        <p className="text-xs font-medium tracking-widest uppercase text-cyan-400 mb-2">Get started</p>
        <h2 className="text-3xl font-light text-zinc-100 leading-tight">
          Create your{" "}
          <span
            className="text-transparent bg-clip-text"
            style={{ backgroundImage: "linear-gradient(90deg, #06b6d4, #3b82f6)" }}
          >
            account
          </span>
        </h2>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="mb-5">
          <label className={LABEL}>First Name</label>
          <input type="text" placeholder="Jane" value={form.firstName} onChange={set("firstName")}
            className={INPUT_BASE + (errors.firstName ? " border-red-500" : "")} />
          {errors.firstName && <p className="text-xs text-red-400 mt-1">{errors.firstName}</p>}
        </div>
        <div className="mb-5">
          <label className={LABEL}>Last Name</label>
          <input type="text" placeholder="Smith" value={form.lastName} onChange={set("lastName")}
            className={INPUT_BASE + (errors.lastName ? " border-red-500" : "")} />
          {errors.lastName && <p className="text-xs text-red-400 mt-1">{errors.lastName}</p>}
        </div>
      </div>

      <FieldGroup label="Email Address" type="email" placeholder="you@company.com"
        value={form.email} onChange={set("email") as (e: React.ChangeEvent<HTMLInputElement>) => void}
        error={errors.email} />

      <div className="mb-5">
        <label className={LABEL}>Role</label>
        <select
          value={form.role}
          onChange={set("role")}
          className="w-full bg-zinc-900 border border-zinc-700 focus:border-cyan-400 outline-none px-4 py-3 text-zinc-300 text-sm transition-all duration-200 rounded-sm cursor-pointer"
        >
          <option value="EVENT_MANAGER">Event Manager</option>
          <option value="GUEST">Guest</option>
        </select>
      </div>

      <FieldGroup label="Password" type="password" placeholder="Min. 6 characters"
        value={form.password} onChange={set("password") as (e: React.ChangeEvent<HTMLInputElement>) => void}
        error={errors.password} />
      <FieldGroup label="Confirm Password" type="password" placeholder="Repeat password"
        value={form.confirm} onChange={set("confirm") as (e: React.ChangeEvent<HTMLInputElement>) => void}
        error={errors.confirm} />

      <GlowButton loading={loading} onClick={handleSubmit}>Create Account</GlowButton>

      <p className="text-center text-xs text-zinc-600 mt-6">
        Already registered?{" "}
        <button onClick={onSwitch} className="text-cyan-400 font-semibold hover:text-cyan-300 transition-colors">
          Sign in
        </button>
      </p>
    </div>
  );
};

/* ─── Background ─────────────────────────────────────────────────── */
const GridBackground: FC = () => (
  <div className="absolute inset-0 overflow-hidden pointer-events-none">
    <div
      className="absolute inset-0"
      style={{
        backgroundImage:
          "linear-gradient(#06b6d4 1px, transparent 1px), linear-gradient(90deg, #06b6d4 1px, transparent 1px)",
        backgroundSize: "60px 60px",
        opacity: 0.04,
      }}
    />
    <div className="absolute -top-40 -left-40 w-96 h-96 rounded-full"
      style={{ background: "radial-gradient(circle, #06b6d4, transparent 70%)", opacity: 0.1 }} />
    <div className="absolute -bottom-40 -right-20 w-80 h-80 rounded-full"
      style={{ background: "radial-gradient(circle, #3b82f6, transparent 70%)", opacity: 0.1 }} />
    <div
      className="absolute left-0 right-0 h-px"
      style={{ background: "linear-gradient(90deg, transparent, #06b6d4, transparent)", top: "35%", opacity: 0.2 }}
    />
  </div>
);

/* ─── Constants ──────────────────────────────────────────────────── */
const STATS: { value: string; label: string }[] = [
  { value: "12K+", label: "Invitations Sent" },
  { value: "98%",  label: "Delivery Rate" },
  { value: "340+", label: "Events Managed" },
];

const FEATURES: string[] = ["QR Codes", "Bulk Upload", "PDF Receipts", "Email Delivery"];

/* ─── App ────────────────────────────────────────────────────────── */
const App: FC = () => {
  const [mode, setMode] = useState<Mode>("login");
  const [showSplash, setShowSplash] = useState<boolean>(true);

  return (
    <div className="min-h-screen bg-zinc-950 flex flex-col" style={{ fontFamily: "'DM Sans', 'Helvetica Neue', sans-serif" }}>

      {showSplash && <SplashScreen onDone={() => setShowSplash(false)} />}

      {/* Nav */}
      <header className="flex items-center justify-between px-10 py-5 border-b border-zinc-800/60">
        <div className="flex items-center gap-3">
          <div
            className="w-8 h-8 flex items-center justify-center"
            style={{ background: "linear-gradient(135deg, #06b6d4, #3b82f6)", boxShadow: "0 0 14px rgba(6,182,212,0.4)" }}
          >
            <svg viewBox="0 0 24 24" fill="none" className="w-4 h-4">
              <path
                d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
                stroke="white" strokeWidth="1.8" strokeLinecap="round"
              />
            </svg>
          </div>
          <span className="text-sm font-bold uppercase text-zinc-100" style={{ letterSpacing: "0.18em" }}>
            InviteFlow
          </span>
          <span className="text-cyan-400 border border-cyan-400/30 px-2 py-0.5 rounded-full font-medium" style={{ fontSize: "10px" }}>
            BETA
          </span>
        </div>

        <nav className="hidden md:flex items-center gap-8">
          {["Features", "Pricing", "About"].map((item) => (
            <a key={item} href="#"
              className="text-xs tracking-widest uppercase text-zinc-500 hover:text-zinc-200 transition-colors font-medium">
              {item}
            </a>
          ))}
        </nav>

        <button
          onClick={() => setMode(mode === "login" ? "register" : "login")}
          className="text-xs tracking-widest uppercase font-semibold text-zinc-300 border border-zinc-700 px-5 py-2 hover:border-cyan-400 hover:text-cyan-400 transition-all duration-200"
        >
          {mode === "login" ? "Register" : "Sign In"}
        </button>
      </header>

      {/* Main */}
      <main className="flex flex-1 items-center justify-center px-6 py-14 relative">
        <GridBackground />

        <div
          className="relative z-10 w-full max-w-5xl grid grid-cols-1 lg:grid-cols-2"
          style={{ boxShadow: "0 0 60px rgba(6,182,212,0.08), 0 25px 50px rgba(0,0,0,0.5)" }}
        >
          {/* Left — Brand panel */}
          <div className="relative bg-zinc-900 border border-zinc-800 px-10 py-14 flex flex-col justify-between overflow-hidden">
            <div className="absolute top-0 left-0 right-0 h-px"
              style={{ background: "linear-gradient(90deg, transparent, #06b6d4, transparent)" }} />
            <div className="absolute -top-24 -right-24 w-64 h-64 rounded-full"
              style={{ background: "radial-gradient(circle, #06b6d4, transparent 70%)", opacity: 0.1 }} />

            <div className="relative z-10">
              <div className="flex items-center gap-2 mb-8">
                <span className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse" />
                <span className="text-xs font-medium tracking-widest uppercase text-cyan-400">Platform Online</span>
              </div>
              <p className="text-xs font-medium tracking-widest uppercase text-zinc-500 mb-3">Invitation Management</p>
              <h1 className="text-4xl font-light leading-tight text-zinc-100 mb-6">
                Orchestrate every<br />
                <span className="font-normal italic text-transparent bg-clip-text"
                  style={{ backgroundImage: "linear-gradient(90deg, #06b6d4, #3b82f6)" }}>
                  invitation,
                </span><br />
                flawlessly.
              </h1>
              <p className="text-zinc-500 text-sm leading-relaxed max-w-xs">
                From intimate gatherings to large-scale corporate events — manage, send, and track invitations with precision.
              </p>
            </div>

            <div className="relative z-10 mt-12 grid grid-cols-3 gap-4 border-t border-zinc-800 pt-8">
              {STATS.map((s) => (
                <div key={s.label}>
                  <p className="text-2xl font-light text-transparent bg-clip-text mb-1"
                    style={{ backgroundImage: "linear-gradient(135deg, #06b6d4, #3b82f6)" }}>
                    {s.value}
                  </p>
                  <p className="text-xs text-zinc-600 leading-tight">{s.label}</p>
                </div>
              ))}
            </div>

            <div className="relative z-10 mt-8 flex flex-wrap gap-2">
              {FEATURES.map((tag) => (
                <span key={tag}
                  className="text-xs border border-zinc-700 text-zinc-500 px-3 py-1 tracking-wide hover:border-cyan-400/50 hover:text-cyan-400 transition-all duration-200 cursor-default">
                  {tag}
                </span>
              ))}
            </div>

            <div className="absolute bottom-0 left-0 right-0 h-px"
              style={{ background: "linear-gradient(90deg, transparent, #3b82f6, transparent)" }} />
          </div>

          {/* Right — Form panel */}
          <div className="bg-zinc-950 border border-l-0 border-zinc-800 flex items-stretch">
            <div className="w-full flex flex-col justify-center">
              {mode === "login"
                ? <LoginForm onSwitch={() => setMode("register")} />
                : <RegisterForm onSwitch={() => setMode("login")} />
              }
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="text-center py-6 border-t border-zinc-800/60">
        <p className="text-xs text-zinc-600 tracking-widest">
          © 2026 InviteFlow ·{" "}
          <a href="#" className="hover:text-zinc-400 transition-colors">Privacy</a> ·{" "}
          <a href="#" className="hover:text-zinc-400 transition-colors">Terms</a>
        </p>
      </footer>
    </div>
  );
};

export default App;
