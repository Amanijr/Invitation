import { FC } from "react";

interface SuccessStateProps {
  title: string;
  subtitle: string;
  action?: string;
  onAction?: () => void;
}

const SuccessState: FC<SuccessStateProps> = ({ title, subtitle, action, onAction }) => (
  <div className="flex flex-col items-center justify-center text-center px-8 py-20">
    <div className="relative mb-8">
      <div
        className="w-16 h-16 rounded-full flex items-center justify-center"
        style={{
          background: "linear-gradient(135deg, #06b6d4, #3b82f6)",
          boxShadow: "0 0 30px rgba(6,182,212,0.4)",
        }}
      >
        <svg
          className="w-7 h-7 text-white"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth={2.5}
        >
          <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
        </svg>
      </div>
      <div
        className="absolute inset-0 rounded-full animate-ping opacity-20"
        style={{ background: "linear-gradient(135deg, #06b6d4, #3b82f6)" }}
      />
    </div>

    <p className="text-xs font-bold tracking-widest uppercase text-cyan-400 mb-2">{title}</p>
    <p className="text-2xl font-light text-zinc-100 mb-6">{subtitle}</p>

    {action && onAction && (
      <button
        onClick={onAction}
        className="text-xs tracking-widest uppercase font-semibold text-zinc-400 hover:text-cyan-400 transition-colors border border-zinc-700 hover:border-cyan-400 px-6 py-2.5"
      >
        {action}
      </button>
    )}
  </div>
);

export default SuccessState;