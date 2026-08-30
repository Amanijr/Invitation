import { lazy, Suspense } from "react";
import { Route, Routes, Navigate } from "react-router-dom";
import { LandingPage } from "./pages/LandingPage";
import { AppShell } from "./components/layout/AppShell";
import { RequireAuth } from "./components/auth/RequireAuth";

const AuthPage = lazy(() => import("./pages/AuthPage"));
const ProfilePage = lazy(() =>
  import("./pages/ProfilePage").then((module) => ({ default: module.ProfilePage }))
);
const AdminDashboardPage = lazy(() =>
  import("./pages/AdminDashboardPage").then((module) => ({ default: module.AdminDashboardPage }))
);
const GuestManagerPage = lazy(() =>
  import("./pages/GuestManagerPage").then((module) => ({ default: module.GuestManagerPage }))
);
const TemplateManagerPage = lazy(() =>
  import("./pages/TemplateManagerPage").then((module) => ({ default: module.TemplateManagerPage }))
);
const TemplateDesignerPage = lazy(() =>
  import("./pages/TemplateDesignerPage").then((module) => ({ default: module.TemplateDesignerPage }))
);
const BulkGenerationPage = lazy(() =>
  import("./pages/BulkGenerationPage").then((module) => ({ default: module.BulkGenerationPage }))
);
const AdminScannerPage = lazy(() =>
  import("./pages/AdminScannerPage").then((module) => ({ default: module.AdminScannerPage }))
);

const GuestInvitePage = lazy(() =>
  import("./pages/GuestInvitePage").then((module) => ({ default: module.GuestInvitePage }))
);

function RouteFallback() {
  return <div className="min-h-dvh bg-background" aria-busy="true" />;
}

function App() {
  return (
    <Suspense fallback={<RouteFallback />}>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/invite/:token" element={<GuestInvitePage />} />
        <Route path="/auth" element={<AuthPage />} />
        <Route element={<RequireAuth />}>
          <Route element={<AppShell />}>
            <Route path="/dashboard" element={<AdminDashboardPage />} />
            <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
            <Route path="/profile" element={<ProfilePage />} />
            <Route path="/guests" element={<GuestManagerPage />} />
            <Route path="/templates" element={<TemplateManagerPage />} />
            <Route path="/templates/:templateId/designer" element={<TemplateDesignerPage />} />
            <Route path="/invitations/generate-bulk" element={<BulkGenerationPage />} />
            <Route path="/admin/scan" element={<AdminScannerPage />} />
            <Route path="/scanner" element={<AdminScannerPage />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}

export default App;
