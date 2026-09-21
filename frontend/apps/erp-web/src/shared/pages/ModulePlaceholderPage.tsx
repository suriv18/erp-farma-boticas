import { ArrowLeft, Construction } from 'lucide-react';
import { Link } from 'react-router';
import { Card } from '@boticas/ui-web';

export function ModulePlaceholderPage({
  description,
  title
}: {
  description: string;
  title: string;
}) {
  return (
    <div className="mx-auto max-w-7xl">
      <p className="text-sm font-semibold text-primary-700 dark:text-primary-400">Módulo ERP</p>
      <h1 className="mt-1 text-3xl font-bold tracking-tight text-neutral-950 dark:text-white">
        {title}
      </h1>
      <Card className="mt-7 grid min-h-80 place-items-center p-8 text-center">
        <div className="max-w-md">
          <div className="mx-auto grid size-14 place-items-center rounded-2xl bg-primary-50 text-primary-700 dark:bg-primary-900/40 dark:text-primary-300">
            <Construction className="size-6" />
          </div>
          <h2 className="mt-5 text-xl font-bold text-neutral-900 dark:text-neutral-50">
            Módulo preparado
          </h2>
          <p className="mt-2 text-sm leading-6 text-neutral-500 dark:text-neutral-400">
            {description}
          </p>
          <Link
            to="/dashboard"
            className="mt-6 inline-flex h-11 items-center justify-center gap-2 rounded-xl border border-neutral-200 bg-white px-4 text-sm font-semibold text-neutral-700 shadow-sm transition-colors hover:border-neutral-300 hover:bg-neutral-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-neutral-500 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-200 dark:hover:border-neutral-600 dark:hover:bg-neutral-800"
          >
            <ArrowLeft className="size-4" />
            Volver al resumen
          </Link>
        </div>
      </Card>
    </div>
  );
}
