import { useEffect, useState } from "react";

function useMedia(query: string): boolean {
  const [matches, setMatches] = useState(() =>
    typeof window !== "undefined" ? window.matchMedia(query).matches : false
  );

  useEffect(() => {
    const mq = window.matchMedia(query);
    const onChange = () => setMatches(mq.matches);
    onChange();
    mq.addEventListener("change", onChange);
    return () => mq.removeEventListener("change", onChange);
  }, [query]);

  return matches;
}

export function useReducedMotion(): boolean {
  return useMedia("(prefers-reduced-motion: reduce)");
}

export function useReducedTransparency(): boolean {
  return useMedia("(prefers-reduced-transparency: reduce)");
}

export function useMoreContrast(): boolean {
  return useMedia("(prefers-contrast: more)");
}
