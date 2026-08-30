import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { AuthMode } from "@/types/auth.types";
import LoginForm from "@/components/auth/LoginForm";
import RegisterForm from "@/components/auth/RegistrationForm";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { getPackage, getTemplate } from "@/lib/catalog";
import { getIntent } from "@/lib/session";

export default function AuthPage() {
  const [params] = useSearchParams();
  const [mode, setMode] = useState<AuthMode>(params.get("mode") === "register" ? "register" : "login");
  const next = params.get("next") || "/admin/dashboard";
  const justRegistered = params.get("registered") === "1";
  const registeredEmail = params.get("email") ?? "";
  const intent = getIntent();
  const chosenTemplate = intent ? getTemplate(intent.templateId) : undefined;
  const chosenName = chosenTemplate?.name ?? intent?.templateName;
  const chosenPackage = intent ? getPackage(intent.packageId) : undefined;

  useEffect(() => {
    setMode(params.get("mode") === "register" ? "register" : "login");
  }, [params]);

  return (
    <div className="grid min-h-screen lg:grid-cols-[minmax(280px,2fr)_3fr]">
      <aside className="station-panel relative hidden flex-col justify-between px-10 py-12 lg:flex">
        <Link to="/" className="press font-display text-2xl font-semibold">
          InviteFlow
        </Link>
        <div>
          <h1 className="font-display text-4xl font-semibold leading-tight">
            {chosenName && chosenPackage
              ? `${chosenName} on ${chosenPackage.name}.`
              : "Named invitations, from the list to the door."}
          </h1>
          <p className="mt-4 max-w-sm text-sm leading-6 text-white/70">
            {chosenName
              ? "Create an account or sign in. We keep this sample and package on the desk."
              : "Templates, guest lists, generation, and door scan — one production job."}
          </p>
        </div>
        <Link to="/" className="press text-xs text-white/40 hover:text-white">
          Back to samples
        </Link>
      </aside>

      <div className="flex flex-col bg-background">
        <header className="chrome relative flex h-16 items-center justify-between px-6">
          <Link to="/" className="press font-display text-lg font-semibold lg:hidden">
            InviteFlow
          </Link>
          <div className="ml-auto flex items-center gap-2">
            <Button variant={mode === "login" ? "secondary" : "ghost"} onClick={() => setMode("login")}>
              Sign in
            </Button>
            <Button variant={mode === "register" ? "secondary" : "ghost"} onClick={() => setMode("register")}>
              Create account
            </Button>
          </div>
        </header>

        <main className="flex flex-1 items-center justify-center px-6 py-16">
          <Card className="w-full max-w-md">
            <CardContent className="p-8">
              {chosenName && chosenPackage ? (
                <p className="mb-6 text-sm leading-6 text-muted-foreground">
                  Holding {chosenName} on {chosenPackage.name}.
                </p>
              ) : null}
              {mode === "login" ? (
                <LoginForm
                  nextPath={next}
                  initialEmail={registeredEmail}
                  justRegistered={justRegistered}
                  onSwitch={() => setMode("register")}
                />
              ) : (
                <RegisterForm nextPath={next} onSwitch={() => setMode("login")} />
              )}
            </CardContent>
          </Card>
        </main>

        <footer className="border-t border-border px-6 py-6">
          <p className="text-center text-xs text-muted-foreground">2026 InviteFlow</p>
        </footer>
      </div>
    </div>
  );
}
