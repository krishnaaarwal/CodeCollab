import { forwardRef, type ButtonHTMLAttributes, type InputHTMLAttributes, type SelectHTMLAttributes, type ReactNode } from 'react';
import { Loader2 } from 'lucide-react';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'ghost' | 'danger';
  loading?: boolean;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant = 'primary', loading = false, className = '', children, disabled, ...props }, ref) => {
    const baseStyle = "inline-flex items-center justify-center gap-2 rounded-md text-[13px] font-medium transition-all focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed";
    const variants = {
      primary: "bg-zinc-100 text-zinc-950 hover:bg-zinc-200 shadow-sm px-4 py-2",
      ghost: "bg-transparent border border-zinc-800 text-zinc-400 hover:bg-zinc-800 hover:text-zinc-100 px-4 py-2",
      danger: "bg-transparent border border-zinc-800 text-red-500 hover:bg-red-500/10 hover:border-red-500/50 px-4 py-2"
    };

    return (
      <button ref={ref} className={`${baseStyle} ${variants[variant]} ${className}`} disabled={disabled || loading} {...props}>
        {loading && <Loader2 className="animate-spin" size={15} />}
        {children}
      </button>
    );
  }
);
Button.displayName = 'Button';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, id, className = '', ...props }, ref) => {
    const inputId = id || props.name || label?.toLowerCase().replace(/\s+/g, '-');
    return (
      <div className="flex w-full flex-col gap-1.5">
        {label && <label htmlFor={inputId} className="text-[12px] font-medium text-zinc-400">{label}</label>}
        <input
          ref={ref}
          id={inputId}
          className={`w-full rounded-md border bg-zinc-900/50 px-3 py-2 text-[14px] text-zinc-100 placeholder:text-zinc-600 focus:outline-none focus:ring-1 transition-all ${
            error ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20' : 'border-zinc-800 focus:border-blue-500 focus:ring-blue-500/20'
          } ${className}`}
          {...props}
        />
        {error && <span className="text-[12px] text-red-400">{error}</span>}
      </div>
    );
  }
);
Input.displayName = 'Input';

interface SelectOption { value: string; label: string; }
interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  options: SelectOption[];
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, options, id, className = '', ...props }, ref) => {
    const selectId = id || props.name || label?.toLowerCase().replace(/\s+/g, '-');
    return (
      <div className="flex w-full flex-col gap-1.5">
        {label && <label htmlFor={selectId} className="text-[12px] font-medium text-zinc-400">{label}</label>}
        <select 
          ref={ref} 
          id={selectId} 
          className={`w-full rounded-md border border-zinc-800 bg-zinc-900/50 px-3 py-2 text-[14px] text-zinc-100 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500/20 ${className}`} 
          {...props}
        >
          {options.map((opt) => <option key={opt.value} value={opt.value} className="bg-zinc-900">{opt.label}</option>)}
        </select>
      </div>
    );
  }
);
Select.displayName = 'Select';

export function Spinner({ size = 20 }: { size?: number }) {
  return <Loader2 className="animate-spin text-zinc-500" size={size} />;
}

export function Badge({ children, tone = 'default' }: { children: ReactNode; tone?: 'default' | 'success' | 'warning' }) {
  const tones = {
    default: "border-zinc-700 bg-zinc-800/50 text-zinc-400",
    success: "border-emerald-500/30 bg-emerald-500/10 text-emerald-400",
    warning: "border-amber-500/30 bg-amber-500/10 text-amber-400"
  };
  return <span className={`inline-flex items-center gap-1.5 rounded-md border px-2 py-0.5 text-[10px] font-medium uppercase tracking-wider ${tones[tone]}`}>{children}</span>;
}