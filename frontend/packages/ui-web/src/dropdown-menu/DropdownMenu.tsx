import type { ReactNode } from 'react';
import { DropdownMenu as RadixDropdownMenu } from 'radix-ui';
import { Link } from 'react-router';
import type { LucideIcon } from 'lucide-react';
import { cn } from '../lib/cn';

export type DropdownMenuProps = {
  trigger: ReactNode;
  children: ReactNode;
};

export function DropdownMenu({ trigger, children }: DropdownMenuProps) {
  return (
    <RadixDropdownMenu.Root>
      <RadixDropdownMenu.Trigger asChild>{trigger}</RadixDropdownMenu.Trigger>
      <RadixDropdownMenu.Portal>
        <RadixDropdownMenu.Content
          align="end"
          sideOffset={8}
          className="z-50 w-64 overflow-hidden rounded-2xl border border-neutral-200 bg-white py-1.5 shadow-xl dark:border-neutral-800 dark:bg-neutral-900"
        >
          {children}
        </RadixDropdownMenu.Content>
      </RadixDropdownMenu.Portal>
    </RadixDropdownMenu.Root>
  );
}

export type DropdownMenuItemProps = {
  icon?: LucideIcon;
  children: ReactNode;
  to?: string;
  tone?: 'default' | 'danger';
  onSelect?: () => void;
};

export function DropdownMenuItem({
  icon: Icon,
  children,
  to,
  tone = 'default',
  onSelect
}: DropdownMenuItemProps) {
  const itemClassName = cn(
    'flex cursor-pointer items-center gap-2.5 px-4 py-2.5 text-sm font-medium outline-none transition-colors',
    tone === 'danger'
      ? 'text-danger-700 hover:bg-danger-50 dark:text-danger-400 dark:hover:bg-danger-900/20'
      : 'text-neutral-700 hover:bg-neutral-50 dark:text-neutral-200 dark:hover:bg-neutral-800'
  );

  if (to) {
    return (
      <RadixDropdownMenu.Item asChild>
        <Link to={to} className={itemClassName}>
          {Icon ? <Icon className="size-4" aria-hidden="true" /> : null}
          {children}
        </Link>
      </RadixDropdownMenu.Item>
    );
  }

  return (
    <RadixDropdownMenu.Item className={itemClassName} onSelect={() => onSelect?.()}>
      {Icon ? <Icon className="size-4" aria-hidden="true" /> : null}
      {children}
    </RadixDropdownMenu.Item>
  );
}

export function DropdownMenuLabel({ children }: { children: ReactNode }) {
  return (
    <RadixDropdownMenu.Label className="px-4 pt-2 pb-1 text-[11px] font-bold tracking-[0.08em] text-neutral-400 uppercase dark:text-neutral-500">
      {children}
    </RadixDropdownMenu.Label>
  );
}

export function DropdownMenuSeparator() {
  return (
    <RadixDropdownMenu.Separator className="my-1.5 h-px bg-neutral-100 dark:bg-neutral-800" />
  );
}
