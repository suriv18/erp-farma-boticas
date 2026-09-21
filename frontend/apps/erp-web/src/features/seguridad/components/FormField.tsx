import type { PropsWithChildren, ReactNode } from 'react';

export type FormFieldProps = PropsWithChildren<{
  label: string;
  htmlFor: string;
  error?: string | undefined;
  hint?: ReactNode;
}>;

export function FormField({ label, htmlFor, error, hint, children }: FormFieldProps) {
  return (
    <div>
      <label
        htmlFor={htmlFor}
        className="text-sm font-semibold text-neutral-700 dark:text-neutral-200"
      >
        {label}
      </label>
      <div className="mt-2">{children}</div>
      {error ? (
        <p
          id={`${htmlFor}-error`}
          role="alert"
          className="text-danger-600 dark:text-danger-400 mt-1.5 text-xs font-medium"
        >
          {error}
        </p>
      ) : null}
      {!error && hint ? (
        <p className="mt-1.5 text-xs text-neutral-500 dark:text-neutral-400">{hint}</p>
      ) : null}
    </div>
  );
}
