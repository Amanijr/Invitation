import { type ChangeEvent, type FC, type FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { validateRegister } from "@/components/utils/validation";
import { apiFetch, readError } from "@/lib/api";
import { signOut } from "@/lib/session";
import { RegisterFormState, FormErrors } from "@/types/auth.types";

interface RegisterFormProps {
  onSwitch: () => void;
  nextPath?: string;
}

const RegisterForm: FC<RegisterFormProps> = ({ onSwitch, nextPath }) => {
  const navigate = useNavigate();
  const [form, setForm] = useState<RegisterFormState>({
    firstName: "",
    lastName: "",
    email: "",
    password: "",
    confirm: "",
    role: "EVENT_MANAGER",
  });
  const [errors, setErrors] = useState<FormErrors>({});
  const [loading, setLoading] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const setField =
    (field: keyof RegisterFormState) =>
    (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      setForm((f) => ({ ...f, [field]: e.target.value }));
    };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const next = validateRegister(form);
    if (Object.keys(next).length) {
      setErrors(next);
      return;
    }
    setErrors({});
    setSubmitError(null);
    setLoading(true);
    try {
      const res = await apiFetch("/auth/register", {
        method: "POST",
        body: JSON.stringify({
          firstName: form.firstName.trim(),
          lastName: form.lastName.trim(),
          email: form.email.trim(),
          password: form.password,
          role: "EVENT_MANAGER",
        }),
      });
      if (!res.ok) {
        setSubmitError(await readError(res));
        return;
      }
      signOut();
      const params = new URLSearchParams({ mode: "login", registered: "1" });
      const email = form.email.trim();
      if (email) params.set("email", email);
      if (nextPath) params.set("next", nextPath);
      navigate(`/auth?${params.toString()}`);
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : "Could not create the account.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div>
        <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Create account</p>
        <h2 className="mt-2 font-display text-2xl font-semibold">Join the desk</h2>
      </div>

      {submitError ? <p className="text-sm text-destructive">{submitError}</p> : null}

      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label htmlFor="first-name">First name</Label>
          <Input id="first-name" value={form.firstName} onChange={setField("firstName")} autoComplete="given-name" />
          {errors.firstName ? <p className="text-xs text-destructive">{errors.firstName}</p> : null}
        </div>
        <div className="space-y-2">
          <Label htmlFor="last-name">Last name</Label>
          <Input id="last-name" value={form.lastName} onChange={setField("lastName")} autoComplete="family-name" />
          {errors.lastName ? <p className="text-xs text-destructive">{errors.lastName}</p> : null}
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="reg-email">Email</Label>
        <Input id="reg-email" type="email" value={form.email} onChange={setField("email")} autoComplete="email" />
        {errors.email ? <p className="text-xs text-destructive">{errors.email}</p> : null}
      </div>

      <div className="space-y-2">
        <Label htmlFor="reg-password">Password</Label>
        <Input
          id="reg-password"
          type="password"
          value={form.password}
          onChange={setField("password")}
          autoComplete="new-password"
        />
        {errors.password ? <p className="text-xs text-destructive">{errors.password}</p> : null}
      </div>

      <div className="space-y-2">
        <Label htmlFor="reg-confirm">Confirm password</Label>
        <Input
          id="reg-confirm"
          type="password"
          value={form.confirm}
          onChange={setField("confirm")}
          autoComplete="new-password"
        />
        {errors.confirm ? <p className="text-xs text-destructive">{errors.confirm}</p> : null}
      </div>

      <Button type="submit" className="w-full" disabled={loading}>
        {loading ? "Creating…" : "Create account"}
      </Button>

      <p className="text-center text-sm text-muted-foreground">
        Already registered?{" "}
        <button type="button" onClick={onSwitch} className="font-medium text-accent hover:underline">
          Sign in
        </button>
      </p>
    </form>
  );
};

export default RegisterForm;
