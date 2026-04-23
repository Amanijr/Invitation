import { FC } from "react";

interface Props {
  label: string;
  type?: string;
  placeholder: string;
  value: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  error?: string;
}

const FieldGroup: FC<Props> = ({
  label,
  type = "text",
  placeholder,
  value,
  onChange,
  error,
}) => {
  return (
    <div className="mb-5">
      <label className="block text-xs font-medium tracking-widest uppercase text-zinc-500 mb-2">
        {label}
      </label>

      <input
        type={type}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        className={`w-full bg-zinc-900 border px-4 py-3 text-zinc-100 text-sm rounded-sm
          ${
            error
              ? "border-red-500"
              : "border-zinc-700 focus:border-cyan-400"
          }`}
      />

      {error && (
        <p className="text-xs text-red-400 mt-1">{error}</p>
      )}
    </div>
  );
};

export default FieldGroup;