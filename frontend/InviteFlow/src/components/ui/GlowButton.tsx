import { FC, ReactNode } from "react";

interface Props {
  children: ReactNode;
  loading?: boolean;
  onClick: () => void;
}

const GlowButton: FC<Props> = ({
  children,
  loading = false,
  onClick,
}) => {
  return (
    <button
      onClick={onClick}
      disabled={loading}
      className="w-full py-3 text-white rounded-sm"
    >
      {loading ? "Processing..." : children}
    </button>
  );
};

export default GlowButton;