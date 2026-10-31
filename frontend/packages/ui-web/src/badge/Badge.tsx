import type { HTMLAttributes } from 'react';
import { cn } from '../lib/cn';

type BadgeTone = 'neutral' | 'success' | 'warning' | 'danger';

const tones: Record<BadgeTone, string> = {
  neutral: 'bg-neutral-100 text-neutral-700 dark:bg-neutral-800 dark:text-neutral-200',
  success: 'bg-success-50 text-success-700 dark:bg-success-900/40 dark:text-success-300',
  warning: 'bg-warning-50 text-warning-800 dark:bg-warning-900/40 dark:text-warning-300',
  danger: 'bg-danger-50 text-danger-700 dark:bg-danger-900/40 dark:text-danger-300'
};

export type BadgeProps = HTMLAttributes<HTMLSpanElement> & { tone?: BadgeTone };

export function Badge({ className, tone = 'neutral', ...props }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold',
        tones[tone],
        className
      )}
      {...props}
    />
  );
}
