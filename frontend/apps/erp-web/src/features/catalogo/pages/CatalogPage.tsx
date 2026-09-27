import {
  ClipboardList,
  FolderTree,
  IdCard,
  Pill,
  Ruler,
  ShieldAlert,
  Store,
  Syringe,
  Tag
} from 'lucide-react';
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
  },
  {
    to: '/catalogo/rubros-comerciales',
    icon: Store,
    title: 'Rubros comerciales',
    description: 'Administra los rubros comerciales del catálogo.'
  },
  {
    to: '/catalogo/condiciones-venta',
    icon: ClipboardList,
    title: 'Condiciones de venta',
    description: 'Administra las condiciones de venta del catálogo.'
  },
  {
    to: '/catalogo/formas-farmaceuticas',
    icon: Pill,
    title: 'Formas farmacéuticas',
    description: 'Administra las formas farmacéuticas del catálogo.'
  },
  {
    to: '/catalogo/vias-administracion',
    icon: Syringe,
    title: 'Vías de administración',
    description: 'Administra las vías de administración del catálogo.'
  },
  {
    to: '/catalogo/unidades-medida',
    icon: Ruler,
    title: 'Unidades de medida',
    description: 'Administra las unidades de medida del catálogo.'
  },
  {
    to: '/catalogo/clasificaciones-controladas',
    icon: ShieldAlert,
    title: 'Clasificaciones controladas',
    description: 'Administra las clasificaciones controladas del catálogo.'
  },
  {
    to: '/catalogo/tipos-documento-identidad',
    icon: IdCard,
    title: 'Tipos de documento de identidad',
    description: 'Administra el catálogo SUNAT de tipos de documento de identidad.'
  }
];

export function CatalogPage() {
  return (
    <div className="mx-auto max-w-7xl">
      <p className="text-primary-700 dark:text-primary-400 text-sm font-semibold">Módulo ERP</p>
      <h1 className="mt-1 text-3xl font-bold tracking-tight text-neutral-950 dark:text-white">
        Catálogo
      </h1>
      <p className="mt-2 text-sm text-neutral-500 dark:text-neutral-400">
        Productos, categorías, marcas y catálogos de soporte regulatorio.
      </p>

      <div className="mt-7 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {sections.map(({ to, icon: Icon, title, description }) => (
          <Link key={to} to={to} className="block focus-visible:outline-none">
            <Card className="hover:border-primary-300 hover:bg-primary-50/40 dark:hover:border-primary-700 dark:hover:bg-primary-900/20 h-full p-6 transition-colors">
              <div className="bg-primary-50 text-primary-700 dark:bg-primary-900/40 dark:text-primary-300 grid size-11 place-items-center rounded-xl">
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
