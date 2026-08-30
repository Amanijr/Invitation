import { createContext, useContext } from "react";

type OverlayCtx = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

export const OverlayOpenContext = createContext<OverlayCtx>({
  open: false,
  onOpenChange: () => undefined,
});

export function useOverlayOpen() {
  return useContext(OverlayOpenContext);
}
