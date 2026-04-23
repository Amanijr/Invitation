import { FC, useState, ChangeEvent } from "react";

import FieldGroup from "../ui/FieldGroup";
import GlowButton from "../ui/GlowButton";
import SuccessState from "../ui/SuccessState";

import { validateRegister } from "../utils/validation";
import { RegisterFormState, FormErrors } from "../../types/auth.types";
interface RegisterFormProps {
  onSwitch: () => void;
}

const LABEL = "block text-xs font-medium tracking-widest uppercase text-zinc-500 mb-2";

const RegisterForm: FC<RegisterFormProps> = ({ onSwitch }) => {
  const [form, setForm] = useState<RegisterFormState>({
    firstName: "",
    lastName: "",
    email: "",
    password: "",
    confirm: "",
    role: "EVENT_MANAGER",
  });
  const [errors, setErrors] = useState<FormErrors>({});
  const [loading, setLoading] = useState<boolean>(false);
  const [success, setSuccess] = useState<boolean>(false);

  const setField =
    (field: keyof RegisterFormState) =>
    (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>): void => {
      setForm((f) => ({ ...f, [field]: e.target.value }));
    };

  const handleSubmit = (): void => {
    const e = validateRegister(form);
    if (Object.keys(e).length) { setErrors(e); return; }
    setErrors({});
    setLoading(true);
    // TODO: replace with real API call → POST /api/v1/auth/register
    setTimeout(() => { setLoading(false); setSuccess(true); }, 1800);
  };

  if (success) return (
    <SuccessState
      title="Account Created"
      subtitle="You're all set."
      action="Sign In →"
      onAction={onSwitch}
    />
  );

  return (
    <div className="px-10 py-10 w-full overflow-y-auto">
      <div className="mb-8">
        <p className="text-xs font-medium tracking-widest uppercase text-cyan-400 mb-2">
          Get started
        </p>
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

      {/* Name row */}
      <div className="grid grid-cols-2 gap-4">
        <div className="mb-5">
          <label className={LABEL}>First Name</label>
          <input
            type="text"
            placeholder="Jane"
            value={form.firstName}
            onChange={setField("firstName")}
            className={
              "w-full bg-zinc-900 border border-zinc-700 focus:border-cyan-400 outline-none px-4 py-3 text-zinc-100 placeholder-zinc-600 text-sm transition-all duration-200 rounded-sm" +
              (errors.firstName ? " border-red-500" : "")
            }
          />
          {errors.firstName && (
            <p className="text-xs text-red-400 mt-1">{errors.firstName}</p>
          )}
        </div>
        <div className="mb-5">
          <label className={LABEL}>Last Name</label>
          <input
            type="text"
            placeholder="Smith"
            value={form.lastName}
            onChange={setField("lastName")}
            className={
              "w-full bg-zinc-900 border border-zinc-700 focus:border-cyan-400 outline-none px-4 py-3 text-zinc-100 placeholder-zinc-600 text-sm transition-all duration-200 rounded-sm" +
              (errors.lastName ? " border-red-500" : "")
            }
          />
          {errors.lastName && (
            <p className="text-xs text-red-400 mt-1">{errors.lastName}</p>
          )}
        </div>
      </div>

      <FieldGroup
        label="Email Address"
        type="email"
        placeholder="you@company.com"
        value={form.email}
        onChange={setField("email") as (e: ChangeEvent<HTMLInputElement>) => void}
        error={errors.email}
      />

      {/* Role selector */}
      <div className="mb-5">
        <label className={LABEL}>Role</label>
        <select
          value={form.role}
          onChange={setField("role")}
          className="w-full bg-zinc-900 border border-zinc-700 focus:border-cyan-400 outline-none px-4 py-3 text-zinc-300 text-sm transition-all duration-200 rounded-sm cursor-pointer"
        >
          <option value="EVENT_MANAGER">Event Manager</option>
          <option value="GUEST">Guest</option>
        </select>
      </div>

      <FieldGroup
        label="Password"
        type="password"
        placeholder="Min. 6 characters"
        value={form.password}
        onChange={setField("password") as (e: ChangeEvent<HTMLInputElement>) => void}
        error={errors.password}
      />
      <FieldGroup
        label="Confirm Password"
        type="password"
        placeholder="Repeat password"
        value={form.confirm}
        onChange={setField("confirm") as (e: ChangeEvent<HTMLInputElement>) => void}
        error={errors.confirm}
      />

      <GlowButton loading={loading} onClick={handleSubmit}>
        Create Account
      </GlowButton>

      <p className="text-center text-xs text-zinc-600 mt-6">
        Already registered?{" "}
        <button
          onClick={onSwitch}
          className="text-cyan-400 font-semibold hover:text-cyan-300 transition-colors"
        >
          Sign in
        </button>
      </p>
    </div>
  );
};

export default RegisterForm;