import type { ComponentPropsWithRef } from 'react';
import type { LucideIcon } from 'lucide-react';
import { cn } from '../lib/cn';

export type InputProps = ComponentPropsWithRef<'input'> & {
  icon?: LucideIcon;
};

export function Input({ className, icon: Icon, ref, ...props }: InputProps) {
  if (!Icon) {
    return (
      <input
        ref={ref}
        className={cn(
          'focus:border-primary-600 focus:ring-primary-100 aria-invalid:border-danger-400 aria-invalid:focus:ring-danger-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none transition placeholder:text-neutral-400 hover:border-neutral-300 focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:placeholder:text-neutral-500 dark:hover:border-neutral-600',
          className
        )}
        {...props}
      />
    );
  }

  return (
    <div className="relative">
      <Icon
        className="pointer-events-none absolute top-1/2 left-3.5 size-4.5 -translate-y-1/2 text-neutral-400"
        aria-hidden="true"
      />
      <input
        ref={ref}
        className={cn(
          'focus:border-primary-600 focus:ring-primary-100 aria-invalid:border-danger-400 aria-invalid:focus:ring-danger-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white pr-4 pl-11 text-sm text-neutral-900 shadow-sm outline-none transition placeholder:text-neutral-400 hover:border-neutral-300 focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:placeholder:text-neutral-500 dark:hover:border-neutral-600',
          className
        )}
        {...props}
      />
    </div>
  );
}
