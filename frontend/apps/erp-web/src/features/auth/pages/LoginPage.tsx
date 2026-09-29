import { ApiError } from '@boticas/api-client';
import { Boxes, Building2, Pill, ShieldCheck, Store, Warehouse } from 'lucide-react';
import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router';
import { LoginForm } from '../components/LoginForm';
import { useAuthSession } from '../model/useAuthSession';
import type { LoginCredentials } from '../schemas/login.schema';

const operationalFeatures = [
  {
    icon: Boxes,
    title: 'Inventario inteligente',
    description: 'Control de lotes, stock mínimo y alertas de vencimiento.'
  },
  {
    icon: Store,
    title: 'Ventas y comprobantes',
    description: 'Boletas y facturas electrónicas en segundos.'
  },
  {
    icon: Warehouse,
    title: 'Reportes en tiempo real',
    description: 'Indicadores por sucursal, turno y vendedor.'
  }
];

const GENERIC_LOGIN_ERROR = 'Credenciales incorrectas o cuenta bloqueada.';

export function LoginPage() {
  const { authenticate } = useAuthSession();
  const location = useLocation();
  const navigate = useNavigate();
  const [submitError, setSubmitError] = useState<string | null>(null);

  async function handleAuthentication(credentials: LoginCredentials) {
    setSubmitError(null);

    try {
      await authenticate(credentials);
    } catch (error) {
      setSubmitError(
        error instanceof ApiError
          ? GENERIC_LOGIN_ERROR
          : 'No se pudo iniciar sesión. Intenta nuevamente.'
      );
      return;
    }

    const locationState = location.state as { from?: unknown } | null;
    const destination =
      typeof locationState?.from === 'string' && locationState.from !== '/login'
        ? locationState.from
        : '/dashboard';

    await navigate(destination, { replace: true });
  }

  return (
    <main className="min-h-screen bg-[#f4f7f5] lg:grid lg:grid-cols-[minmax(0,1.08fr)_minmax(480px,0.92fr)] dark:bg-neutral-950">
      <section
        className="relative hidden min-h-screen overflow-hidden p-10 text-white lg:flex lg:flex-col xl:p-14"
        style={{ backgroundImage: 'linear-gradient(160deg, #0d9488 0%, #065f46 55%, #022c22 100%)' }}
      >
        <div
          className="pointer-events-none absolute inset-0 opacity-80"
          style={{
            backgroundImage:
              'radial-gradient(circle at 12% 18%, rgba(255, 255, 255, 0.10), transparent 28%), radial-gradient(circle at 82% 78%, rgba(255, 255, 255, 0.08), transparent 27%)'
          }}
        />
        <div className="pointer-events-none absolute top-24 -right-24 size-72 rounded-full border border-white/10" />
        <div className="pointer-events-none absolute top-40 -right-4 size-44 rounded-full border border-white/10" />

        <div className="relative flex items-center gap-3">
          <div className="bg-white text-success-800 shadow-success-950/30 grid size-11 place-items-center rounded-2xl shadow-lg">
            <Pill className="size-5" aria-hidden="true" />
          </div>
          <div>
            <p className="text-lg font-black tracking-tight">FarmaVita</p>
            <p className="text-success-100/65 text-xs font-medium">
              Sistema de gestión de boticas
            </p>
          </div>
        </div>

        <div className="relative my-auto max-w-xl py-16">
          <div className="border-white/20 text-success-50 inline-flex items-center gap-2 rounded-full border bg-white/10 px-3 py-1.5 text-xs font-semibold backdrop-blur">
            <ShieldCheck className="text-white size-3.5" aria-hidden="true" />
            Plataforma para boticas y farmacias
          </div>
          <h1 className="mt-6 max-w-lg text-4xl leading-[1.08] font-black tracking-[-0.035em] text-balance xl:text-5xl">
            Gestiona tu botica con precisión y confianza.
          </h1>
          <p className="text-success-50/70 mt-5 max-w-lg text-base leading-7 xl:text-lg">
            Controla inventario, lotes y vencimientos, ventas y caja de todas tus sucursales desde
            un solo lugar.
          </p>

          <ul className="mt-8 space-y-4" aria-label="Funcionalidades de la plataforma">
            {operationalFeatures.map(({ icon: Icon, title, description }) => (
              <li key={title} className="flex items-start gap-3.5">
                <span className="bg-white/15 text-white grid size-10 shrink-0 place-items-center rounded-xl">
                  <Icon className="size-5" aria-hidden="true" />
                </span>
                <div>
                  <p className="text-white text-sm font-bold">{title}</p>
                  <p className="text-success-50/70 mt-0.5 text-sm leading-6">{description}</p>
                </div>
              </li>
            ))}
          </ul>

          <div className="mt-10 grid max-w-lg grid-cols-2 gap-3">
            <div className="rounded-2xl border border-white/10 bg-white/8 p-4 backdrop-blur-sm">
              <p className="text-success-50/70 text-xs font-semibold">Ventas de hoy</p>
              <p className="mt-2 text-xl font-black">S/ 8,452.30</p>
              <p className="text-success-200 mt-1 text-[11px] font-bold">▲ 12.4%</p>
            </div>
            <div className="rounded-2xl border border-white/10 bg-white/8 p-4 backdrop-blur-sm">
              <p className="text-success-50/70 text-xs font-semibold">Por vencer (30 días)</p>
              <p className="mt-2 text-xl font-black">7 lotes</p>
              <p className="text-success-200 mt-1 text-[11px] font-bold">Amoxicilina 500mg</p>
            </div>
          </div>
        </div>

        <div className="text-success-50/50 relative flex items-center justify-between gap-5 border-t border-white/10 pt-6 text-xs">
          <span>Datos cifrados de extremo a extremo · Respaldo diario automático</span>
          <span className="flex items-center gap-1.5">
            <Building2 className="size-3.5" aria-hidden="true" />
            © 2026 FarmaVita
          </span>
        </div>
      </section>

      <section className="flex min-h-screen flex-col px-5 py-6 sm:px-8 lg:px-12 xl:px-20">
        <div className="flex items-center gap-3 lg:hidden">
          <div className="bg-success-900 text-white grid size-10 place-items-center rounded-xl">
            <Pill className="size-4.5" aria-hidden="true" />
          </div>
          <div>
            <p className="font-black tracking-tight text-neutral-950 dark:text-white">
              FarmaVita
            </p>
            <p className="text-[11px] text-neutral-500 dark:text-neutral-400">
              Sistema de gestión de boticas
            </p>
          </div>
        </div>

        <div className="my-auto w-full max-w-md self-center py-10">
          <div className="bg-success-100 text-success-800 dark:bg-success-900/40 dark:text-success-300 mb-7 inline-flex size-12 items-center justify-center rounded-2xl lg:hidden">
            <ShieldCheck className="size-5" aria-hidden="true" />
          </div>
          <h2 className="text-3xl font-black tracking-[-0.03em] text-neutral-950 sm:text-4xl dark:text-white">
            Bienvenido de nuevo 👋
          </h2>
          <p className="mt-3 max-w-sm text-sm leading-6 text-neutral-500 dark:text-neutral-400">
            Ingresa tus credenciales para acceder al panel de tu botica.
          </p>

          <LoginForm onAuthenticate={handleAuthentication} submitError={submitError} />
        </div>

        <footer className="flex flex-col items-center justify-between gap-2 border-t border-neutral-200/70 pt-5 text-[11px] text-neutral-400 sm:flex-row dark:border-neutral-800 dark:text-neutral-500">
          <span>© 2026 FarmaVita · Términos · Privacidad</span>
          <span>v2.4.0</span>
        </footer>
      </section>
    </main>
  );
}
