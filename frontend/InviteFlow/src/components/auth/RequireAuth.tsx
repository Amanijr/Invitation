import { Navigate, Outlet, useLocation } from "react-router-dom";
import { isSignedIn } from "@/lib/session";

export function RequireAuth() {
  const location = useLocation();
  if (!isSignedIn()) {
    const next = encodeURIComponent(location.pathname + location.search);
    return <Navigate to={`/auth?next=${next}`} replace />;
  }
  return <Outlet />;
}
