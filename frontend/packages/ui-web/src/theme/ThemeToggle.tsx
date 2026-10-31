import type { ComponentPropsWithRef } from 'react';
import { Moon, Sun } from 'lucide-react';
import { cn } from '../lib/cn';
import { useTheme } from './useTheme';

export type ThemeToggleProps = Omit<ComponentPropsWithRef<'button'>, 'onClick' | 'children'>;

export function ThemeToggle({ className, ref, ...props }: ThemeToggleProps) {
  const { theme, setTheme } = useTheme();
  const isDark = theme === 'dark';

  return (
    <button
      ref={ref}
      type="button"
      aria-label={isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
      onClick={() => setTheme(isDark ? 'light' : 'dark')}
      className={cn(
        'grid size-10 place-items-center rounded-xl text-neutral-500 transition-colors hover:bg-neutral-100 hover:text-neutral-700 dark:text-neutral-400 dark:hover:bg-neutral-800 dark:hover:text-neutral-100',
        className
      )}
      {...props}
    >
      {isDark ? (
        <Sun className="size-4.5" aria-hidden="true" />
      ) : (
        <Moon className="size-4.5" aria-hidden="true" />
      )}
    </button>
  );
}
