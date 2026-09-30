import type { PropsWithChildren } from 'react';

export function Aviso({ children }: PropsWithChildren) {
  return (
    <p
      role="status"
      className="border-warning-200 bg-warning-50 text-warning-800 dark:border-warning-800 dark:bg-warning-900/30 dark:text-warning-300 mt-6 rounded-lg border px-4 py-3 text-sm"
    >
      {children}
    </p>
  );
}
