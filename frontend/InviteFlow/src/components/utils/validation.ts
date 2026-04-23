import { FormErrors, LoginFormState, RegisterFormState } from "../../types/auth.types";

export const validateLogin = (form: LoginFormState): FormErrors => {
  const errors: FormErrors = {};
  if (!form.email.includes("@")) errors.email = "Enter a valid email address";
  if (form.password.length < 6) errors.password = "Password must be at least 6 characters";
  return errors;
};

export const validateRegister = (form: RegisterFormState): FormErrors => {
  const errors: FormErrors = {};
  if (!form.firstName.trim()) errors.firstName = "Required";
  if (!form.lastName.trim()) errors.lastName = "Required";
  if (!form.email.includes("@")) errors.email = "Enter a valid email";
  if (form.password.length < 6) errors.password = "Min 6 characters";
  if (form.password !== form.confirm) errors.confirm = "Passwords do not match";
  return errors;
};