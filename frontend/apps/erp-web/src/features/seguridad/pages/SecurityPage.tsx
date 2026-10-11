import { KeyRound, Laptop, LayoutGrid, ShieldCheck, Users2, Users } from 'lucide-react';
import { Link } from 'react-router';
import { Card } from '@boticas/ui-web';

const sections = [
  {
    to: '/seguridad/usuarios',
    icon: Users,
    title: 'Usuarios',
    description: 'Administra las cuentas de acceso al sistema.'
  },
  {
    to: '/seguridad/roles',
    icon: ShieldCheck,
    title: 'Roles',
    description: 'Administra los roles y sus permisos asociados.'
  },
  {
    to: '/seguridad/permisos',
    icon: KeyRound,
    title: 'Permisos',
    description: 'Consulta los permisos disponibles en el sistema.'
  },
  {
    to: '/seguridad/sesiones',
    icon: Users2,
    title: 'Sesiones',
    description: 'Consulta y revoca sesiones locales de acceso activas.'
  },
  {
    to: '/seguridad/dispositivos',
    icon: Laptop,
    title: 'Dispositivos',
    description: 'Administra la confianza de los dispositivos de tienda.'
  },
  {
    to: '/seguridad/modulos',
    icon: LayoutGrid,
    title: 'Módulos',
    description: 'Catálogo de módulos que agrupan los permisos del sistema.'
  }
];

export function SecurityPage() {
  return (
    <div className="mx-auto max-w-7xl">
      <p className="text-primary-700 dark:text-primary-400 text-sm font-semibold">Módulo ERP</p>
      <h1 className="mt-1 text-3xl font-bold tracking-tight text-neutral-950 dark:text-white">
        Seguridad
      </h1>
      <p className="mt-2 text-sm text-neutral-500 dark:text-neutral-400">
        Usuarios, roles, permisos y control de accesos.
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
