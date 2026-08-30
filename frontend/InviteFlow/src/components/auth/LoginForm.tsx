import { type FC, type FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { validateLogin } from "@/components/utils/validation";
import { apiFetch, readError, readJson } from "@/lib/api";
import { signIn, type AuthResponse } from "@/lib/session";
import { LoginFormState, FormErrors } from "@/types/auth.types";

interface LoginFormProps {
  onSwitch: () => void;
  nextPath?: string;
  initialEmail?: string;
  justRegistered?: boolean;
}

const LoginForm: FC<LoginFormProps> = ({
  onSwitch,
  nextPath = "/admin/dashboard",
  initialEmail = "",
  justRegistered = false,
}) => {
  const navigate = useNavigate();
  const [form, setForm] = useState<LoginFormState>({ email: initialEmail, password: "" });
  const [errors, setErrors] = useState<FormErrors>({});
  const [loading, setLoading] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const next = validateLogin(form);
    if (Object.keys(next).length) {
      setErrors(next);
      return;
    }
    setErrors({});
    setSubmitError(null);
    setLoading(true);
    try {
      const res = await apiFetch("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email: form.email.trim(), password: form.password }),
      });
      if (!res.ok) {
        setSubmitError(await readError(res));
        return;
      }
      const auth = await readJson<AuthResponse>(res);
      signIn(auth);
      navigate(nextPath);
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : "Could not sign in.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div>
        <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Sign in</p>
        <h2 className="mt-2 font-display text-2xl font-semibold">Open the desk</h2>
      </div>

      {justRegistered ? (
        <p className="text-sm leading-6 text-foreground">Account created. Sign in to open the desk.</p>
      ) : null}

      {submitError ? <p className="text-sm text-destructive">{submitError}</p> : null}

      <div className="space-y-2">
        <Label htmlFor="login-email">Email</Label>
        <Input
          id="login-email"
          type="email"
          placeholder="you@studio.com"
          value={form.email}
          onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
          autoComplete="email"
        />
        {errors.email ? <p className="text-xs text-destructive">{errors.email}</p> : null}
      </div>

      <div className="space-y-2">
        <Label htmlFor="login-password">Password</Label>
        <Input
          id="login-password"
          type="password"
          value={form.password}
          onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
          autoComplete="current-password"
        />
        {errors.password ? <p className="text-xs text-destructive">{errors.password}</p> : null}
      </div>

      <Button type="submit" className="w-full" disabled={loading}>
        {loading ? "Signing in…" : "Sign in"}
      </Button>

      <p className="text-center text-sm text-muted-foreground">
        No account?{" "}
        <button type="button" onClick={onSwitch} className="font-medium text-accent hover:underline">
          Create one
        </button>
      </p>
    </form>
  );
};

export default LoginForm;
