import { htmlLangFor } from "@/lib/locale";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import "./index.css";
import App from "./App.tsx";
import { TooltipProvider } from "@/components/ui/tooltip";
import { Toaster } from "@/components/ui/sonner";

document.documentElement.lang = htmlLangFor();

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <BrowserRouter>
      <TooltipProvider>
        <App />
        <Toaster />
      </TooltipProvider>
    </BrowserRouter>
  </StrictMode>
);
