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
import { Button, cn } from '@boticas/ui-web';

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
        <div className="grid size-10 place-items-center rounded-xl bg-white text-teal-800 shadow-lg shadow-teal-950/20">
          <span className="text-lg font-black">B+</span>
        </div>
        <div>
          <p className="font-bold tracking-tight text-white">ERP Boticas</p>
          <p className="text-xs text-teal-100/70">Gestión farmacéutica</p>
        </div>
      </div>

      <nav aria-label="Navegación principal" className="flex-1 space-y-1 p-4">
        <p className="mb-3 px-3 text-[11px] font-bold tracking-[0.18em] text-teal-100/50 uppercase">
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
                    ? 'bg-white text-teal-900 shadow-sm'
                    : 'text-teal-50/75 hover:bg-white/10 hover:text-white'
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
        <p className="mt-1 text-sm text-teal-50/70">Botica Central · Lima</p>
      </div>
    </>
  );
}

export function AppShell() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 flex-col bg-teal-950 lg:flex">
        <SidebarContent />
      </aside>

      {mobileMenuOpen ? (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            className="absolute inset-0 bg-slate-950/50 backdrop-blur-sm"
            aria-label="Cerrar menú"
            onClick={() => setMobileMenuOpen(false)}
          />
          <aside className="relative flex h-full w-72 flex-col bg-teal-950 shadow-2xl">
            <button
              className="absolute top-5 right-4 rounded-lg p-2 text-teal-50 hover:bg-white/10"
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
        <header className="sticky top-0 z-20 flex h-20 items-center gap-4 border-b border-slate-200/80 bg-white/90 px-4 backdrop-blur-xl sm:px-6 lg:px-8">
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
            <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
            <input
              type="search"
              placeholder="Buscar productos, clientes o ventas..."
              className="h-11 w-full rounded-xl border border-slate-200 bg-slate-50 pr-4 pl-10 text-sm outline-none placeholder:text-slate-400 focus:border-teal-600 focus:ring-3 focus:ring-teal-100"
            />
          </div>

          <div className="ml-auto flex items-center gap-2">
            <Button variant="ghost" size="sm" className="relative px-2" aria-label="Notificaciones">
              <Bell className="size-5" />
              <span className="absolute top-1.5 right-1.5 size-2 rounded-full bg-rose-500 ring-2 ring-white" />
            </Button>
            <button className="flex items-center gap-3 rounded-xl p-1.5 text-left hover:bg-slate-50">
              <div className="grid size-9 place-items-center rounded-xl bg-teal-100 text-sm font-bold text-teal-800">
                MR
              </div>
              <div className="hidden sm:block">
                <p className="text-sm font-semibold text-slate-800">María Rojas</p>
                <p className="text-xs text-slate-500">Administradora</p>
              </div>
              <ChevronDown className="hidden size-4 text-slate-400 sm:block" />
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
