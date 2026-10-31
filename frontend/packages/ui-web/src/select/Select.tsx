import type { ComponentPropsWithRef } from 'react';
import { cn } from '../lib/cn';

export type SelectProps = ComponentPropsWithRef<'select'>;

export function Select({ className, ref, ...props }: SelectProps) {
  return (
    <select
      ref={ref}
      className={cn(
        'focus:border-primary-600 focus:ring-primary-100 aria-invalid:border-danger-400 aria-invalid:focus:ring-danger-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm transition outline-none hover:border-neutral-300 focus:ring-4 disabled:cursor-not-allowed disabled:bg-neutral-50 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:hover:border-neutral-600',
        className
      )}
      {...props}
    />
  );
}
