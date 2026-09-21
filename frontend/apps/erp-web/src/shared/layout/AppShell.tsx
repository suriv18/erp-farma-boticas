import {
  Bell,
  Boxes,
  Building2,
  ChevronDown,
  LayoutDashboard,
  Menu,
  MonitorSmartphone,
  PackageSearch,
  ReceiptText,
  Search,
  ShieldCheck,
  ShoppingCart,
  Users,
  WalletCards,
  X,
  type LucideIcon
} from 'lucide-react';
import { useState } from 'react';
import { NavLink, Outlet } from 'react-router';
import { Button, cn, ThemeToggle } from '@boticas/ui-web';

type NavigationItem = {
  label: string;
  to: string;
  icon: LucideIcon;
};

const navigation: NavigationItem[] = [
  { label: 'Resumen', to: '/dashboard', icon: LayoutDashboard },
  { label: 'Catálogo', to: '/catalogo', icon: PackageSearch },
  { label: 'Inventario', to: '/inventario', icon: Boxes },
  { label: 'Compras', to: '/compras', icon: ShoppingCart },
  { label: 'Ventas', to: '/ventas', icon: ReceiptText },
  { label: 'Punto de venta', to: '/pos', icon: MonitorSmartphone },
  { label: 'Caja', to: '/caja', icon: WalletCards },
  { label: 'Clientes', to: '/clientes', icon: Users },
  { label: 'Seguridad', to: '/seguridad', icon: ShieldCheck },
  { label: 'Organización', to: '/organizacion', icon: Building2 }
];

function SidebarContent({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <>
      <div className="flex h-20 items-center gap-3 border-b border-white/10 px-6">
        <div className="text-primary-800 shadow-primary-950/20 grid size-10 place-items-center rounded-xl bg-white shadow-lg">
          <span className="text-lg font-black">B+</span>
        </div>
        <div>
          <p className="font-bold tracking-tight text-white">ERP Boticas</p>
          <p className="text-primary-100/70 text-xs">Gestión farmacéutica</p>
        </div>
      </div>

      <nav aria-label="Navegación principal" className="flex-1 space-y-1 p-4">
        <p className="text-primary-100/50 mb-3 px-3 text-[11px] font-bold tracking-[0.18em] uppercase">
          Operaciones
        </p>
        {navigation.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={onNavigate}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors',
                  isActive
                    ? 'text-primary-900 bg-white shadow-sm'
                    : 'text-primary-50/75 hover:bg-white/10 hover:text-white'
                )
              }
            >
              <Icon className="size-4.5" aria-hidden="true" />
              {item.label}
            </NavLink>
          );
        })}
      </nav>

      <div className="m-4 rounded-2xl border border-white/10 bg-white/5 p-4">
        <p className="text-xs font-semibold text-white">Sucursal activa</p>
        <p className="text-primary-50/70 mt-1 text-sm">Botica Central · Lima</p>
      </div>
    </>
  );
}

export function AppShell() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <div className="min-h-screen bg-neutral-50 text-neutral-900 dark:bg-neutral-950 dark:text-neutral-100">
      <aside className="bg-primary-950 fixed inset-y-0 left-0 z-30 hidden w-64 flex-col lg:flex">
        <SidebarContent />
      </aside>

      {mobileMenuOpen ? (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            className="absolute inset-0 bg-neutral-950/50 backdrop-blur-sm"
            aria-label="Cerrar menú"
            onClick={() => setMobileMenuOpen(false)}
          />
          <aside className="bg-primary-950 relative flex h-full w-72 flex-col shadow-2xl">
            <button
              className="text-primary-50 absolute top-5 right-4 rounded-lg p-2 hover:bg-white/10"
              aria-label="Cerrar menú"
              onClick={() => setMobileMenuOpen(false)}
            >
              <X className="size-5" />
            </button>
            <SidebarContent onNavigate={() => setMobileMenuOpen(false)} />
          </aside>
        </div>
      ) : null}

      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 flex h-20 items-center gap-4 border-b border-neutral-200/80 bg-white/90 px-4 backdrop-blur-xl sm:px-6 lg:px-8 dark:border-neutral-800 dark:bg-neutral-900/90">
          <Button
            variant="ghost"
            size="sm"
            className="px-2 lg:hidden"
            aria-label="Abrir menú"
            onClick={() => setMobileMenuOpen(true)}
          >
            <Menu className="size-5" />
          </Button>

          <div className="relative hidden max-w-md flex-1 md:block">
            <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-neutral-400" />
            <input
              type="search"
              placeholder="Buscar productos, clientes o ventas..."
              className="focus:border-primary-600 focus:ring-primary-100 h-11 w-full rounded-xl border border-neutral-200 bg-neutral-50 pr-4 pl-10 text-sm outline-none placeholder:text-neutral-400 focus:ring-3 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100 dark:placeholder:text-neutral-500"
            />
          </div>

          <div className="ml-auto flex items-center gap-2">
            <ThemeToggle />
            <Button variant="ghost" size="sm" className="relative px-2" aria-label="Notificaciones">
              <Bell className="size-5" />
              <span className="bg-danger-500 absolute top-1.5 right-1.5 size-2 rounded-full ring-2 ring-white dark:ring-neutral-900" />
            </Button>
            <button className="flex items-center gap-3 rounded-xl p-1.5 text-left hover:bg-neutral-50 dark:hover:bg-neutral-800">
              <div className="bg-primary-100 text-primary-800 dark:bg-primary-900/50 dark:text-primary-200 grid size-9 place-items-center rounded-xl text-sm font-bold">
                MR
              </div>
              <div className="hidden sm:block">
                <p className="text-sm font-semibold text-neutral-800 dark:text-neutral-100">
                  María Rojas
                </p>
                <p className="text-xs text-neutral-500 dark:text-neutral-400">Administradora</p>
              </div>
              <ChevronDown className="hidden size-4 text-neutral-400 sm:block" />
            </button>
          </div>
        </header>

        <main className="p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
