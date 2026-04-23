import { FC, useState } from "react";
import {
  SplashScreen,
  AuthNavbar,
  BrandPanel,
  GridBackground,
  LoginForm,
  RegisterForm,
} from "../components";
import { AuthMode } from "../types/auth.types";

const AuthPage: FC = () => {
  const [mode, setMode] = useState<AuthMode>("login");
  const [showSplash, setShowSplash] = useState<boolean>(true);

  const toggleMode = (): void =>
    setMode((m) => (m === "login" ? "register" : "login"));

  return (
    <div
      className="min-h-screen bg-zinc-950 flex flex-col"
      style={{ fontFamily: "'DM Sans', 'Helvetica Neue', sans-serif" }}
    >
      {/* Splash animation on first load */}
      {showSplash && <SplashScreen onDone={() => setShowSplash(false)} />}

      {/* Top navigation bar */}
      <AuthNavbar mode={mode} onToggleMode={toggleMode} />

      {/* Main content */}
      <main className="flex flex-1 items-center justify-center px-6 py-14 relative">
        <GridBackground />

        <div
          className="relative z-10 w-full max-w-5xl grid grid-cols-1 lg:grid-cols-2"
          style={{
            boxShadow:
              "0 0 60px rgba(6,182,212,0.08), 0 25px 50px rgba(0,0,0,0.5)",
          }}
        >
          {/* Left — branding */}
          <BrandPanel />

          {/* Right — form */}
          <div className="bg-zinc-950 border border-l-0 border-zinc-800 flex items-stretch">
            <div className="w-full flex flex-col justify-center">
              {mode === "login" ? (
                <LoginForm onSwitch={() => setMode("register")} />
              ) : (
                <RegisterForm onSwitch={() => setMode("login")} />
              )}
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="text-center py-6 border-t border-zinc-800/60">
        <p className="text-xs text-zinc-600 tracking-widest">
          © 2026 InviteFlow ·{" "}
          <a href="#" className="hover:text-zinc-400 transition-colors">
            Privacy
          </a>{" "}
          ·{" "}
          <a href="#" className="hover:text-zinc-400 transition-colors">
            Terms
          </a>
        </p>
      </footer>
    </div>
  );
};

export default AuthPage;