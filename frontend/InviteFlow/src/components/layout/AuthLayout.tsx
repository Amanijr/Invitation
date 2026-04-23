import { ReactNode } from "react";

import GridBackground from "../ui/GridBackground";
import BrandPanel from "../marketing/BrandPanel";

interface AuthLayoutProps {
  children: ReactNode;
}

const AuthLayout = ({ children }: AuthLayoutProps) => {
  return (
    <main className="flex flex-1 items-center justify-center px-6 py-14 relative">
      
      {/* Background (exact from original) */}
      <GridBackground />

      {/* Main container */}
      <div
        className="relative z-10 w-full max-w-5xl grid grid-cols-1 lg:grid-cols-2"
        style={{
          boxShadow:
            "0 0 60px rgba(6,182,212,0.08), 0 25px 50px rgba(0,0,0,0.5)",
        }}
      >
        {/* LEFT — Brand Panel (unchanged UI) */}
        <BrandPanel />

        {/* RIGHT — Form Panel */}
        <div className="bg-zinc-950 border border-l-0 border-zinc-800 flex items-stretch">
          <div className="w-full flex flex-col justify-center">
            {children}
          </div>
        </div>
      </div>
    </main>
  );
};

export default AuthLayout;