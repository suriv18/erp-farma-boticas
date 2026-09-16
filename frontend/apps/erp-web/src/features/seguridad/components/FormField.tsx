import type { PropsWithChildren, ReactNode } from 'react';

export type FormFieldProps = PropsWithChildren<{
  label: string;
  htmlFor: string;
  error?: string;
  hint?: ReactNode;
}>;

export function FormField({ label, htmlFor, error, hint, children }: FormFieldProps) {
  return (
    <div>
      <label htmlFor={htmlFor} className="text-sm font-semibold text-slate-700">
        {label}
      </label>
      <div className="mt-2">{children}</div>
      {error ? (
        <p id={`${htmlFor}-error`} role="alert" className="mt-1.5 text-xs font-medium text-rose-600">
          {error}
        </p>
      ) : null}
      {!error && hint ? <p className="mt-1.5 text-xs text-slate-500">{hint}</p> : null}
    </div>
  );
}
