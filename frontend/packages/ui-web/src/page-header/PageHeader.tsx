import type { ReactNode } from 'react';

export type PageHeaderProps = {
  title: string;
  description: string;
  context: ReactNode;
  actions?: ReactNode;
};

export function PageHeader({ title, description, context, actions }: PageHeaderProps) {
  return (
    <header className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
      <div className="min-w-0">
        <p className="text-primary-700 dark:text-primary-400 text-sm font-semibold">{context}</p>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-neutral-950 dark:text-white">
          {title}
        </h1>
        <p className="mt-2 text-sm text-neutral-500 dark:text-neutral-400">{description}</p>
      </div>
      <div className="flex shrink-0 flex-wrap gap-2">{actions}</div>
    </header>
  );
}
