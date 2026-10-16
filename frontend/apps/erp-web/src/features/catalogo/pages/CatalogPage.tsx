import { useState } from 'react';
import {
  ArrowRight,
  Barcode,
  ClipboardList,
  FlaskConical,
  FolderTree,
  IdCard,
  Package,
  Pill,
  Ruler,
  ShieldAlert,
  Store,
  Syringe,
  Tag
} from 'lucide-react';
import { Link } from 'react-router';
import { Button, Card, ListFilters } from '@boticas/ui-web';

const sections = [
  {
    to: '/catalogo/marcas',
    icon: Tag,
    title: 'Marcas',
    description: 'Marcas comerciales.'
  },
  {
    to: '/catalogo/categorias',
    icon: FolderTree,
    title: 'Categorías',
    description: 'Familias y jerarquía de productos.'
  },
  {
    to: '/catalogo/rubros-comerciales',
    icon: Store,
    title: 'Rubros comerciales',
    description: 'Rubros para clasificar tu oferta.'
  },
  {
    to: '/catalogo/condiciones-venta',
    icon: ClipboardList,
    title: 'Condiciones de venta',
    description: 'Requisitos para la dispensación.'
  },
  {
    to: '/catalogo/formas-farmaceuticas',
    icon: Pill,
    title: 'Formas farmacéuticas',
    description: 'Tabletas, cápsulas y otras formas.'
  },
  {
    to: '/catalogo/vias-administracion',
    icon: Syringe,
    title: 'Vías de administración',
    description: 'Oral, tópica y otras vías.'
  },
  {
    to: '/catalogo/unidades-medida',
    icon: Ruler,
    title: 'Unidades de medida',
    description: 'Unidades para venta y medición.'
  },
  {
    to: '/catalogo/clasificaciones-controladas',
    icon: ShieldAlert,
    title: 'Clasificaciones controladas',
    description: 'Clasificación de sustancias controladas.'
  },
  {
    to: '/catalogo/tipos-documento-identidad',
    icon: IdCard,
    title: 'Tipos de documento de identidad',
    description: 'Documentos de identidad según SUNAT.'
  },
  {
    to: '/catalogo/principios-activos',
    icon: FlaskConical,
    title: 'Principios activos',
    description: 'Sustancias de los productos regulados.'
  },
  {
    to: '/catalogo/productos-regulados',
    icon: Package,
    title: 'Productos regulados',
    description: 'Registro sanitario y composición.'
  },
  {
    to: '/catalogo/skus',
    icon: Barcode,
    title: 'SKU comerciales',
    description: 'Presentaciones y códigos de barras.'
  }
];

const groups = [
  {
    title: 'Productos y composición',
    description: 'Gestiona lo que vendes y su información regulatoria.',
    paths: ['skus', 'productos-regulados', 'principios-activos']
  },
  {
    title: 'Organización comercial',
    description: 'Clasifica los productos para encontrarlos con facilidad.',
    paths: ['marcas', 'categorias', 'rubros-comerciales']
  },
  {
    title: 'Catálogos de soporte',
    description: 'Mantén los valores compartidos por los formularios del sistema.',
    paths: [
      'condiciones-venta',
      'formas-farmaceuticas',
      'vias-administracion',
      'unidades-medida',
      'clasificaciones-controladas',
      'tipos-documento-identidad'
    ]
  }
];

const normalize = (value: string) =>
  value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim();

export function CatalogPage() {
  const [search, setSearch] = useState('');
  const [selectedGroup, setSelectedGroup] = useState('Todos');
  const activeGroup = groups.find((group) => group.title === selectedGroup);
  const visible = sections.filter(
    (section) =>
      normalize(section.title + ' ' + section.description).includes(normalize(search)) &&
      (!activeGroup || activeGroup.paths.some((path) => section.to === '/catalogo/' + path))
  );
  return (
    <div className="erp-page space-y-5">
      <header>
        <h1 className="text-2xl font-bold tracking-tight text-neutral-950 dark:text-white">
          Catálogo
        </h1>
        <p className="mt-1 text-sm text-neutral-600 dark:text-neutral-300">
          Encuentra y administra la información de tus productos.
        </p>
      </header>
      <ListFilters
        label="Buscar catálogo"
        placeholder="Por ejemplo: marcas, unidades o productos"
        value={search}
        onValueChange={setSearch}
      >
        {search ? (
          <Button variant="ghost" onClick={() => setSearch('')}>
            Limpiar búsqueda
          </Button>
        ) : null}
      </ListFilters>
      <div className="flex flex-wrap gap-2" role="group" aria-label="Filtrar por grupo">
        {['Todos', ...groups.map((group) => group.title)].map((title) => (
          <Button
            key={title}
            variant={selectedGroup === title ? 'primary' : 'secondary'}
            aria-pressed={selectedGroup === title}
            onClick={() => setSelectedGroup(title)}
            className="h-auto min-h-11 py-2 text-left"
          >
            {title}
          </Button>
        ))}
      </div>
      <p
        role="status"
        aria-atomic="true"
        className="text-sm text-neutral-500 dark:text-neutral-400"
      >
        {visible.length} de {sections.length} catálogos
      </p>
      {groups.map((group) => {
        const items = group.paths.flatMap((path) =>
          visible.filter((section) => section.to === '/catalogo/' + path)
        );
        if (!items.length) return null;
        return (
          <section key={group.title} aria-label={group.title}>
            <h2 className="text-sm font-semibold text-neutral-900 dark:text-neutral-50">
              {group.title}
            </h2>
            <div className="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
              {items.map(({ to, icon: Icon, title, description }) => (
                <Link
                  key={to}
                  to={to}
                  className="group focus-visible:outline-primary-600 block rounded-xl focus-visible:outline-2 focus-visible:outline-offset-4"
                >
                  <Card className="group-hover:border-primary-300 group-hover:bg-primary-50/40 group-focus-visible:border-primary-500 dark:group-hover:border-primary-700 dark:group-hover:bg-primary-900/20 flex h-full min-h-24 items-center gap-3 rounded-xl p-4 transition-colors motion-reduce:transition-none">
                    <div className="bg-primary-50 text-primary-700 dark:bg-primary-900/40 dark:text-primary-300 grid size-9 shrink-0 place-items-center rounded-lg">
                      <Icon className="size-5" aria-hidden="true" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <h3 className="text-sm font-semibold text-neutral-900 dark:text-neutral-50">
                        {title}
                      </h3>
                      <p className="mt-1 text-xs leading-5 text-neutral-500 dark:text-neutral-400">
                        {description}
                      </p>
                    </div>
                    <ArrowRight
                      className="group-hover:text-primary-600 group-focus-visible:text-primary-600 size-4 shrink-0 text-neutral-400 motion-safe:transition-transform motion-safe:group-hover:translate-x-1 motion-safe:group-focus-visible:translate-x-1"
                      aria-hidden="true"
                    />
                  </Card>
                </Link>
              ))}
            </div>
          </section>
        );
      })}
      {!visible.length ? (
        <Card className="p-8 text-center">
          <h2 className="font-semibold text-neutral-900 dark:text-white">
            No se encontraron catálogos
          </h2>
          <p className="mt-2 text-sm text-neutral-500 dark:text-neutral-400">
            Prueba con otro nombre o restablece los filtros para ver todas las opciones.
          </p>
          <Button
            className="mt-4"
            variant="secondary"
            onClick={() => {
              setSearch('');
              setSelectedGroup('Todos');
            }}
          >
            Ver todos los catálogos
          </Button>
        </Card>
      ) : null}
    </div>
  );
}
