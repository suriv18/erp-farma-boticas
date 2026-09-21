import { FolderTree, Tag } from 'lucide-react';
import { Link } from 'react-router';
import { Card } from '@boticas/ui-web';

const sections = [
  {
    to: '/catalogo/marcas',
    icon: Tag,
    title: 'Marcas',
    description: 'Administra las marcas comerciales del catálogo.'
  },
  {
    to: '/catalogo/categorias',
    icon: FolderTree,
    title: 'Categorías',
    description: 'Administra las categorías de productos del catálogo.'
  }
];

export function CatalogPage() {
  return (
    <div className="mx-auto max-w-7xl">
      <p className="text-sm font-semibold text-primary-700 dark:text-primary-400">Módulo ERP</p>
      <h1 className="mt-1 text-3xl font-bold tracking-tight text-neutral-950 dark:text-white">
        Catálogo
      </h1>
      <p className="mt-2 text-sm text-neutral-500 dark:text-neutral-400">
        Productos, categorías, marcas y laboratorios.
      </p>

      <div className="mt-7 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {sections.map(({ to, icon: Icon, title, description }) => (
          <Link key={to} to={to} className="block focus-visible:outline-none">
            <Card className="h-full p-6 transition-colors hover:border-primary-300 hover:bg-primary-50/40 dark:hover:border-primary-700 dark:hover:bg-primary-900/20">
              <div className="grid size-11 place-items-center rounded-xl bg-primary-50 text-primary-700 dark:bg-primary-900/40 dark:text-primary-300">
                <Icon className="size-5" aria-hidden="true" />
              </div>
              <h2 className="mt-4 text-lg font-bold text-neutral-900 dark:text-neutral-50">
                {title}
              </h2>
              <p className="mt-1 text-sm leading-6 text-neutral-500 dark:text-neutral-400">
                {description}
              </p>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  );
}
