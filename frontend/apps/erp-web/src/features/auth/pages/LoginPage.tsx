import { ApiError } from '@boticas/api-client';
import { Boxes, Building2, Check, Pill, ShieldCheck, Store, Warehouse } from 'lucide-react';
import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router';
import { LoginForm } from '../components/LoginForm';
import { useAuthSession } from '../model/useAuthSession';
import type { LoginCredentials } from '../schemas/login.schema';

const operationalBenefits = [
  'Inventario y lotes en tiempo real',
  'Ventas y caja con trazabilidad',
  'Permisos por empresa y sucursal'
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
      <section className="bg-primary-950 relative hidden min-h-screen overflow-hidden p-10 text-white lg:flex lg:flex-col xl:p-14">
        <div
          className="pointer-events-none absolute inset-0 opacity-80"
          style={{
            backgroundImage:
              'radial-gradient(circle at 12% 18%, rgba(45, 212, 191, 0.22), transparent 28%), radial-gradient(circle at 82% 78%, rgba(250, 204, 21, 0.18), transparent 27%)'
          }}
        />
        <div className="pointer-events-none absolute top-24 -right-24 size-72 rounded-full border border-white/10" />
        <div className="pointer-events-none absolute top-40 -right-4 size-44 rounded-full border border-white/10" />

        <div className="relative flex items-center gap-3">
          <div className="bg-warning-300 text-primary-950 shadow-primary-950/30 grid size-11 place-items-center rounded-2xl shadow-lg">
            <Pill className="size-5" aria-hidden="true" />
          </div>
          <div>
            <p className="text-lg font-black tracking-tight">ERP Boticas</p>
            <p className="text-primary-100/65 text-xs font-medium">
              Gestión farmacéutica inteligente
            </p>
          </div>
        </div>

        <div className="relative my-auto max-w-xl py-16">
          <div className="border-primary-300/20 text-primary-50 inline-flex items-center gap-2 rounded-full border bg-white/10 px-3 py-1.5 text-xs font-semibold backdrop-blur">
            <ShieldCheck className="text-warning-300 size-3.5" aria-hidden="true" />
            Operación segura y centralizada
          </div>
          <h1 className="mt-6 max-w-lg text-4xl leading-[1.08] font-black tracking-[-0.035em] text-balance xl:text-5xl">
            Todo lo que tu botica necesita, en un solo lugar.
          </h1>
          <p className="text-primary-50/70 mt-5 max-w-lg text-base leading-7 xl:text-lg">
            Controla sucursales, productos, stock y ventas con información confiable para decidir
            mejor cada día.
          </p>

          <ul className="mt-8 space-y-3.5" aria-label="Beneficios de la plataforma">
            {operationalBenefits.map((benefit) => (
              <li
                key={benefit}
                className="text-primary-50 flex items-center gap-3 text-sm font-medium"
              >
                <span className="bg-warning-300 text-primary-950 grid size-6 place-items-center rounded-full">
                  <Check className="size-3.5 stroke-[3]" aria-hidden="true" />
                </span>
                {benefit}
              </li>
            ))}
          </ul>

          <div className="mt-10 grid max-w-lg grid-cols-3 gap-3">
            <div className="rounded-2xl border border-white/10 bg-white/8 p-4 backdrop-blur-sm">
              <Store className="text-warning-300 size-5" aria-hidden="true" />
              <p className="mt-3 text-xl font-bold">12</p>
              <p className="text-primary-50/60 mt-0.5 text-[11px]">Sucursales</p>
            </div>
            <div className="rounded-2xl border border-white/10 bg-white/8 p-4 backdrop-blur-sm">
              <Warehouse className="text-warning-300 size-5" aria-hidden="true" />
              <p className="mt-3 text-xl font-bold">8.4k</p>
              <p className="text-primary-50/60 mt-0.5 text-[11px]">Unidades</p>
            </div>
            <div className="rounded-2xl border border-white/10 bg-white/8 p-4 backdrop-blur-sm">
              <Boxes className="text-warning-300 size-5" aria-hidden="true" />
              <p className="mt-3 text-xl font-bold">99.9%</p>
              <p className="text-primary-50/60 mt-0.5 text-[11px]">Disponibilidad</p>
            </div>
          </div>
        </div>

        <div className="text-primary-50/50 relative flex items-center justify-between gap-5 border-t border-white/10 pt-6 text-xs">
          <span>© 2026 ERP Boticas</span>
          <span className="flex items-center gap-1.5">
            <Building2 className="size-3.5" aria-hidden="true" />
            Botica Central · Lima
          </span>
        </div>
      </section>

      <section className="flex min-h-screen flex-col px-5 py-6 sm:px-8 lg:px-12 xl:px-20">
        <div className="flex items-center gap-3 lg:hidden">
          <div className="bg-primary-900 text-warning-300 grid size-10 place-items-center rounded-xl">
            <Pill className="size-4.5" aria-hidden="true" />
          </div>
          <div>
            <p className="font-black tracking-tight text-neutral-950 dark:text-white">
              ERP Boticas
            </p>
            <p className="text-[11px] text-neutral-500 dark:text-neutral-400">
              Gestión farmacéutica
            </p>
          </div>
        </div>

        <div className="my-auto w-full max-w-md self-center py-10">
          <div className="bg-primary-100 text-primary-800 dark:bg-primary-900/40 dark:text-primary-300 mb-7 inline-flex size-12 items-center justify-center rounded-2xl lg:hidden">
            <ShieldCheck className="size-5" aria-hidden="true" />
          </div>
          <p className="text-primary-700 dark:text-primary-400 text-sm font-bold">
            Bienvenido de nuevo
          </p>
          <h2 className="mt-2 text-3xl font-black tracking-[-0.03em] text-neutral-950 sm:text-4xl dark:text-white">
            Ingresa a tu cuenta
          </h2>
          <p className="mt-3 max-w-sm text-sm leading-6 text-neutral-500 dark:text-neutral-400">
            Utiliza las credenciales asignadas por el administrador de tu organización.
          </p>

          <LoginForm onAuthenticate={handleAuthentication} submitError={submitError} />
        </div>

        <footer className="flex flex-col items-center justify-between gap-2 border-t border-neutral-200/70 pt-5 text-[11px] text-neutral-400 sm:flex-row dark:border-neutral-800 dark:text-neutral-500">
          <span>Privacidad y tratamiento de datos</span>
          <span>Versión 1.0.0</span>
        </footer>
      </section>
    </main>
  );
}
