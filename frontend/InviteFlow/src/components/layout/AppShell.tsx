import { useEffect, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import {
  BarChart3,
  LayoutTemplate,
  Menu,
  Moon,
  Printer,
  ScanLine,
  Sun,
  Users,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { useMoreContrast, useReducedMotion, useReducedTransparency } from "@/hooks/useReducedMotion";
import { cn } from "@/lib/utils";
import { getSession, signOut, type Session } from "@/lib/session";
import { PersonAvatar } from "@/components/layout/PersonAvatar";

const NAV = [
  { to: "/admin/dashboard", label: "Analytics", icon: BarChart3 },
  { to: "/guests", label: "Guests", icon: Users },
  { to: "/templates", label: "Templates", icon: LayoutTemplate },
  { to: "/invitations/generate-bulk", label: "Generate", icon: Printer },
  { to: "/admin/scan", label: "Scanner", icon: ScanLine },
];

function useTheme() {
  const [dark, setDark] = useState(() => {
    if (typeof document === "undefined") return false;
    return document.documentElement.classList.contains("dark");
  });

  useEffect(() => {
    const stored = localStorage.getItem("inviteflow-theme");
    const next = stored === "dark";
    document.documentElement.classList.toggle("dark", next);
    setDark(next);
  }, []);

  const toggle = () => {
    const next = !document.documentElement.classList.contains("dark");
    document.documentElement.classList.toggle("dark", next);
    localStorage.setItem("inviteflow-theme", next ? "dark" : "light");
    setDark(next);
  };

  return { dark, toggle };
}

function NavItems({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <nav className="flex flex-col gap-1" aria-label="Primary">
      {NAV.map((item) => {
        const Icon = item.icon;
        return (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === "/admin/dashboard"}
            onClick={onNavigate}
            className={({ isActive }) =>
              cn(
                "press flex h-11 items-center gap-3 rounded-md px-3 text-sm font-semibold",
                "text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
                "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
                isActive && "bg-sidebar-accent text-sidebar-accent-foreground"
              )
            }
          >
            <Icon className="size-4 shrink-0" />
            {item.label}
          </NavLink>
        );
      })}
    </nav>
  );
}

function sessionDisplayName(session: Session): string {
  const name = `${session.firstName ?? ""} ${session.lastName ?? ""}`.trim();
  return name || session.email;
}

function ProfileLink({ session, onNavigate }: { session: Session; onNavigate?: () => void }) {
  return (
    <NavLink
      to="/profile"
      onClick={onNavigate}
      className={({ isActive }) =>
        cn(
          "press flex items-center gap-3 rounded-md px-3 py-2",
          "text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
          "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
          isActive && "bg-sidebar-accent text-sidebar-accent-foreground"
        )
      }
    >
      <PersonAvatar size="sm" />
      <span className="min-w-0 text-left">
        <span className="block truncate text-sm font-semibold">{sessionDisplayName(session)}</span>
        <span className="block truncate text-xs font-normal text-sidebar-foreground/70">{session.email}</span>
      </span>
    </NavLink>
  );
}

export function AppShell() {
  const location = useLocation();
  const navigate = useNavigate();
  const { dark, toggle } = useTheme();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [session, setSession] = useState<Session | null>(() => getSession());
  const reducedTransparency = useReducedTransparency();
  const moreContrast = useMoreContrast();
  const reducedMotion = useReducedMotion();
  const isDesigner = location.pathname.includes("/designer");

  useEffect(() => {
    const refresh = () => setSession(getSession());
    window.addEventListener("inviteflow-session", refresh);
    window.addEventListener("storage", refresh);
    return () => {
      window.removeEventListener("inviteflow-session", refresh);
      window.removeEventListener("storage", refresh);
    };
  }, []);

  useEffect(() => {
    document.documentElement.classList.toggle("reduce-transparency", reducedTransparency);
    document.documentElement.classList.toggle("more-contrast", moreContrast);
    document.documentElement.classList.toggle("reduce-motion", reducedMotion);
  }, [reducedTransparency, moreContrast, reducedMotion]);

  return (
    <div className="flex min-h-dvh bg-background">
      <aside className="sidebar-material sticky top-0 hidden h-dvh w-60 shrink-0 flex-col md:flex">
        <div className="flex h-16 items-center px-6">
          <NavLink
            to="/admin/dashboard"
            className="press font-display text-lg font-semibold tracking-tight text-sidebar-foreground"
          >
            InviteFlow
          </NavLink>
        </div>
        <div className="flex-1 overflow-y-auto p-4">
          <p className="mb-2 px-3 text-xs font-semibold text-sidebar-foreground">
            Production
          </p>
          <NavItems />
        </div>
        <div className="space-y-2 p-4">
          {session ? (
            <>
              <ProfileLink session={session} />
              <button
                type="button"
                onClick={() => {
                  signOut();
                  navigate("/auth");
                }}
                className="press flex h-11 w-full items-center rounded-md px-3 text-sm font-semibold text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
              >
                Sign out
              </button>
            </>
          ) : (
            <NavLink
              to="/auth"
              className="press flex h-11 items-center rounded-md px-3 text-sm font-semibold text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
            >
              Sign in
            </NavLink>
          )}
        </div>
      </aside>

      <div className="relative min-w-0 flex-1">
        <header className="chrome fixed inset-x-0 top-0 z-20 flex h-16 items-center gap-3 px-4 md:left-60 md:px-8">
          <Button
            variant="ghost"
            size="icon"
            className="md:hidden"
            onClick={() => setMobileOpen(true)}
            aria-label="Open navigation"
          >
            <Menu className="size-5" />
          </Button>
          <span className="font-display text-base font-semibold md:hidden">InviteFlow</span>
          <div className="ml-auto flex items-center gap-2">
            <Button
              variant="ghost"
              size="icon"
              onClick={toggle}
              aria-label={dark ? "Switch to light mode" : "Switch to dark mode"}
            >
              {dark ? <Sun className="size-4" /> : <Moon className="size-4" />}
            </Button>
          </div>
        </header>

        <main
          className={cn(
            isDesigner
              ? "min-h-[calc(100dvh-4rem)] overflow-hidden pt-16"
              : "px-4 pb-8 pt-24 md:px-8"
          )}
        >
          <Outlet key={session?.userId ?? "signed-out"} />
        </main>
      </div>

      <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
        <SheetContent side="left" className="sidebar-material p-0 text-sidebar-foreground">
          <SheetHeader>
            <SheetTitle className="text-sidebar-foreground">InviteFlow</SheetTitle>
          </SheetHeader>
          <div className="flex h-full flex-col">
            <div className="flex-1 p-4">
              <NavItems onNavigate={() => setMobileOpen(false)} />
            </div>
            {session ? (
              <div className="space-y-2 p-4">
                <ProfileLink session={session} onNavigate={() => setMobileOpen(false)} />
                <button
                  type="button"
                  onClick={() => {
                    setMobileOpen(false);
                    signOut();
                    navigate("/auth");
                  }}
                  className="press flex h-11 w-full items-center rounded-md px-3 text-sm font-semibold text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
                >
                  Sign out
                </button>
              </div>
            ) : null}
          </div>
        </SheetContent>
      </Sheet>
    </div>
  );
}
