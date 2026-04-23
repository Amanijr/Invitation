import { FC, useState } from "react";

import FieldGroup from "../ui/FieldGroup";
import GlowButton from "../ui/GlowButton";
import SuccessState from "../ui/SuccessState";

import { validateLogin } from "../utils/validation";
import { LoginFormState, FormErrors } from "../../types/auth.types";
interface LoginFormProps {
  onSwitch: () => void;
}

const LoginForm: FC<LoginFormProps> = ({ onSwitch }) => {
  const [form, setForm] = useState<LoginFormState>({ email: "", password: "" });
  const [errors, setErrors] = useState<FormErrors>({});
  const [loading, setLoading] = useState<boolean>(false);
  const [success, setSuccess] = useState<boolean>(false);

  const handleSubmit = (): void => {
    const e = validateLogin(form);
    if (Object.keys(e).length) { setErrors(e); return; }
    setErrors({});
    setLoading(true);
    // TODO: replace with real API call → POST /api/v1/auth/login
    setTimeout(() => { setLoading(false); setSuccess(true); }, 1600);
  };

  if (success) return <SuccessState title="Authenticated" subtitle="Welcome back." />;

  return (
    <div className="px-10 py-12 w-full">
      <div className="mb-10">
        <p className="text-xs font-medium tracking-widest uppercase text-cyan-400 mb-2">
          Welcome back
        </p>
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

      <FieldGroup
        label="Email Address"
        type="email"
        placeholder="you@company.com"
        value={form.email}
        onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
        error={errors.email}
      />
      <FieldGroup
        label="Password"
        type="password"
        placeholder="••••••••"
        value={form.password}
        onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
        error={errors.password}
      />

      <div className="flex justify-end mb-8">
        <button className="text-xs text-zinc-500 hover:text-cyan-400 tracking-wide transition-colors">
          Forgot password?
        </button>
      </div>

      <GlowButton loading={loading} onClick={handleSubmit}>
        Sign In
      </GlowButton>

      <p className="text-center text-xs text-zinc-600 mt-6">
        No account?{" "}
        <button
          onClick={onSwitch}
          className="text-cyan-400 font-semibold hover:text-cyan-300 transition-colors"
        >
          Create one
        </button>
      </p>
    </div>
  );
};

export default LoginForm;