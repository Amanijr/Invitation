export type Phase = "enter" | "hold" | "exit";
export type AuthMode = "login" | "register";
export type UserRole = "EVENT_MANAGER" | "GUEST";

export interface LoginFormState {
  email: string;
  password: string;
}

export interface RegisterFormState {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirm: string;
  role: UserRole;
}

export interface FormErrors {
  [key: string]: string;
}