import type { ComponentPropsWithRef } from 'react';
import type { LucideIcon } from 'lucide-react';
import { cn } from '../lib/cn';

export type IconButtonTone = 'default' | 'danger';
export type IconButtonSize = 'sm' | 'md';

export type IconButtonProps = Omit<ComponentPropsWithRef<'button'>, 'aria-label'> & {
  icon: LucideIcon;
  label: string;
  tone?: IconButtonTone;
  size?: IconButtonSize;
};

const tones: Record<IconButtonTone, string> = {
  default:
    'text-neutral-600 hover:bg-neutral-100 hover:text-neutral-900 focus-visible:outline-neutral-500 dark:text-neutral-300 dark:hover:bg-neutral-800 dark:hover:text-white',
  danger:
    'text-danger-700 hover:bg-danger-50 focus-visible:outline-danger-700 dark:text-danger-400 dark:hover:bg-danger-900/30'
};

const sizes: Record<IconButtonSize, string> = {
  sm: 'size-8',
  md: 'size-9'
};

export function iconButtonClassName(
  tone: IconButtonTone = 'default',
  size: IconButtonSize = 'sm'
): string {
  return cn(
    'inline-flex items-center justify-center rounded-lg transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-50',
    tones[tone],
    sizes[size]
  );
}

export function IconButton({
  className,
  icon: Icon,
  label,
  ref,
  size = 'sm',
  tone = 'default',
  type = 'button',
  ...props
}: IconButtonProps) {
  return (
    <button
      ref={ref}
      type={type}
      aria-label={label}
      title={label}
      className={cn(iconButtonClassName(tone, size), className)}
      {...props}
    >
      <Icon className="size-4.5" aria-hidden="true" />
    </button>
  );
}
