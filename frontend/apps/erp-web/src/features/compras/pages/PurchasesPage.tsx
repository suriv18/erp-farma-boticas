import { Truck } from 'lucide-react';
import { Link } from 'react-router';
import { Card, PageHeader } from '@boticas/ui-web';

const sections = [
  {
    to: '/compras/proveedores',
    icon: Truck,
    title: 'Proveedores',
    description: 'Administra los laboratorios, importadores y distribuidores.'
  }
];

export function PurchasesPage() {
  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Compras"
        context="Operaciones / Compras"
        description="Proveedores, órdenes y recepción de mercadería."
      />
      <div className="mt-7 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {sections.map(({ to, icon: Icon, title, description }) => (
          <Link key={to} to={to} className="block focus-visible:outline-none">
            <Card className="hover:border-primary-300 hover:bg-primary-50/40 dark:hover:border-primary-700 dark:hover:bg-primary-900/20 h-full p-6 transition-colors">
              <div className="bg-primary-50 text-primary-700 dark:bg-primary-900/40 dark:text-primary-300 grid size-11 place-items-center rounded-xl">
                <Icon className="size-5" aria-hidden="true" />
              </div>
              <h2 className="mt-4 text-lg font-semibold text-neutral-950 dark:text-white">
                {title}
              </h2>
              <p className="mt-1 text-sm text-neutral-500 dark:text-neutral-400">{description}</p>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  );
}
