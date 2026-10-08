import type { PropsWithChildren } from 'react';

export function DatoItem({ label, children }: PropsWithChildren<{ label: string }>) {
  return (
    <div>
      <dt className="text-xs font-semibold tracking-wide text-neutral-500 uppercase dark:text-neutral-400">
        {label}
      </dt>
      <dd className="mt-1 text-sm text-neutral-900 dark:text-neutral-100">{children}</dd>
    </div>
  );
}
