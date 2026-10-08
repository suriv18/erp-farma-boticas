import type { PropsWithChildren } from 'react';

export function FormSection({ title, children }: PropsWithChildren<{ title: string }>) {
  return (
    <fieldset className="space-y-4 rounded-xl border border-neutral-200 p-4 dark:border-neutral-800">
      <legend className="px-2 text-sm font-semibold text-neutral-700 dark:text-neutral-200">
        {title}
      </legend>
      {children}
    </fieldset>
  );
}
