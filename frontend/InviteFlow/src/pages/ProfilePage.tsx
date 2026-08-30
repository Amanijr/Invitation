import { type FormEvent, useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { PageHeader } from "@/components/layout/PageHeader";
import { PersonAvatar } from "@/components/layout/PersonAvatar";
import { Alert } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { apiFetch, apiJson, readError, readJson } from "@/lib/api";
import { signIn, type AuthResponse } from "@/lib/session";

interface Profile {
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  role?: string;
}

function roleLabel(role?: string): string {
  if (role === "ADMIN") return "Admin";
  if (role === "EVENT_MANAGER") return "Event manager";
  return role?.replaceAll("_", " ") || "Desk";
}

export function ProfilePage() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const next = await apiJson<Profile>("/users/me");
        if (cancelled) return;
        setProfile(next);
        setFirstName(next.firstName ?? "");
        setLastName(next.lastName ?? "");
        setEmail(next.email ?? "");
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "Could not load your profile.");
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const displayName = useMemo(() => {
    const name = `${firstName.trim()} ${lastName.trim()}`.trim();
    return name || profile?.email || "Your name";
  }, [firstName, lastName, profile?.email]);

  const emailChanging = profile != null && email.trim().toLowerCase() !== profile.email.toLowerCase();
  const passwordChanging = newPassword.length > 0 || confirmPassword.length > 0;
  const needsCurrentPassword = emailChanging || passwordChanging;

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setFormError(null);
    if (!firstName.trim() || !lastName.trim()) {
      setFormError("First and last name are required.");
      return;
    }
    if (!email.trim().includes("@")) {
      setFormError("Enter a valid email address.");
      return;
    }
    if (newPassword && newPassword.length < 6) {
      setFormError("New password must be at least 6 characters.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setFormError("New password and confirmation do not match.");
      return;
    }
    if (needsCurrentPassword && !currentPassword) {
      setFormError("Enter your current password to change email or password.");
      return;
    }

    setSaving(true);
    try {
      const res = await apiFetch("/users/me", {
        method: "PUT",
        body: JSON.stringify({
          firstName: firstName.trim(),
          lastName: lastName.trim(),
          email: email.trim(),
          currentPassword: currentPassword || undefined,
          newPassword: newPassword || undefined,
        }),
      });
      if (!res.ok) {
        setFormError(await readError(res));
        return;
      }
      const auth = await readJson<AuthResponse>(res);
      signIn(auth);
      setProfile({
        userId: auth.userId,
        firstName: auth.firstName ?? firstName.trim(),
        lastName: auth.lastName ?? lastName.trim(),
        email: auth.email,
        role: auth.role ?? profile?.role,
      });
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      toast.success("Saved changes");
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "Could not save your profile.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="mx-auto max-w-5xl">
      <PageHeader title="Profile" description="Your name on the desk. Guests never see this." />

      {error ? <Alert variant="destructive">{error}</Alert> : null}

      {loading ? (
        <div className="grid gap-8 lg:grid-cols-[13rem_minmax(0,1fr)]">
          <div className="flex flex-col items-center gap-4 lg:items-start">
            <Skeleton className="size-28 rounded-full" />
            <Skeleton className="h-8 w-40" />
          </div>
          <Skeleton className="h-96 rounded-md" />
        </div>
      ) : profile ? (
        <div className="grid items-start gap-8 lg:grid-cols-[13rem_minmax(0,1fr)]">
          <div className="flex flex-col items-center text-center lg:items-start lg:text-left">
            <PersonAvatar size="lg" />
            <h2 className="mt-5 font-display text-2xl font-semibold tracking-tight text-foreground">
              {displayName}
            </h2>
            <p className="mt-2 font-mono text-xs uppercase tracking-[0.18em] text-primary">
              {roleLabel(profile.role)}
            </p>
            <p className="mt-2 max-w-full truncate text-sm text-muted-foreground">
              {email.trim() || profile.email}
            </p>
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Your details</CardTitle>
              <CardDescription>Role is set when the account is created and cannot be changed here.</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleSubmit} className="space-y-6">
                {formError ? <p className="text-sm text-destructive">{formError}</p> : null}

                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="profile-first-name">First name</Label>
                    <Input
                      id="profile-first-name"
                      value={firstName}
                      onChange={(e) => setFirstName(e.target.value)}
                      autoComplete="given-name"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="profile-last-name">Last name</Label>
                    <Input
                      id="profile-last-name"
                      value={lastName}
                      onChange={(e) => setLastName(e.target.value)}
                      autoComplete="family-name"
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="profile-email">Email</Label>
                  <Input
                    id="profile-email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    autoComplete="email"
                  />
                </div>

                <div className="border-t border-border pt-6">
                  <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Password</p>
                  <p className="mt-1 text-sm leading-6 text-muted-foreground">
                    Leave blank to keep your current password. Changing email or password needs the current one.
                  </p>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="profile-current-password">Current password</Label>
                  <Input
                    id="profile-current-password"
                    type="password"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    autoComplete="current-password"
                    required={needsCurrentPassword}
                  />
                </div>

                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="profile-new-password">New password</Label>
                    <Input
                      id="profile-new-password"
                      type="password"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      autoComplete="new-password"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="profile-confirm-password">Confirm new password</Label>
                    <Input
                      id="profile-confirm-password"
                      type="password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      autoComplete="new-password"
                    />
                  </div>
                </div>

                <Button type="submit" loading={saving}>
                  Save changes
                </Button>
              </form>
            </CardContent>
          </Card>
        </div>
      ) : null}
    </div>
  );
}
