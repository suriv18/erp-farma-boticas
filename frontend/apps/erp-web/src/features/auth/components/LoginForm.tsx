import { zodResolver } from '@hookform/resolvers/zod';
import {
  ArrowRight,
  CheckCircle2,
  CircleHelp,
  Eye,
  EyeOff,
  KeyRound,
  LoaderCircle,
  LockKeyhole,
  Mail,
  QrCode,
  ShieldCheck
} from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { loginSchema, type LoginCredentials } from '../schemas/login.schema';

type LoginFormProps = {
  onAuthenticate: (credentials: LoginCredentials) => Promise<void>;
  submitError?: string | null;
};

export function LoginForm({ onAuthenticate, submitError }: LoginFormProps) {
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [recoveryVisible, setRecoveryVisible] = useState(false);
  const {
    formState: { errors, isSubmitting },
    handleSubmit,
    register
  } = useForm<LoginCredentials>({
    defaultValues: { email: '', password: '', remember: false },
    mode: 'onTouched',
    resolver: zodResolver(loginSchema)
  });

  const submitLogin = handleSubmit(onAuthenticate);

  return (
    <form
      className="mt-8 space-y-5"
      noValidate
      onSubmit={(event) => {
        void submitLogin(event);
      }}
    >
      {submitError ? (
        <p
          role="alert"
          className="border-danger-200 bg-danger-50 text-danger-700 dark:border-danger-800 dark:bg-danger-900/30 dark:text-danger-300 rounded-xl border px-3.5 py-3 text-xs font-medium"
        >
          {submitError}
        </p>
      ) : null}

      <div>
        <label
          htmlFor="email"
          className="text-sm font-semibold text-neutral-700 dark:text-neutral-200"
        >
          Usuario o correo electrónico
        </label>
        <div className="relative mt-2">
          <Mail
            className="pointer-events-none absolute top-1/2 left-3.5 size-4.5 -translate-y-1/2 text-neutral-400"
            aria-hidden="true"
          />
          <input
            id="email"
            type="email"
            autoComplete="username"
            autoFocus
            placeholder="ej. jperez@farmavita.pe"
            aria-describedby={errors.email ? 'email-error' : undefined}
            aria-invalid={Boolean(errors.email)}
            className="focus:border-success-600 focus:ring-success-100 aria-invalid:border-danger-400 aria-invalid:focus:ring-danger-100 dark:focus:ring-success-900/40 h-12 w-full rounded-xl border border-neutral-200 bg-white pr-4 pl-11 text-sm text-neutral-900 shadow-sm transition outline-none placeholder:text-neutral-400 hover:border-neutral-300 focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:placeholder:text-neutral-500 dark:hover:border-neutral-600"
            {...register('email')}
          />
        </div>
        {errors.email ? (
          <p
            id="email-error"
            role="alert"
            className="text-danger-600 dark:text-danger-400 mt-1.5 text-xs font-medium"
          >
            {errors.email.message}
          </p>
        ) : null}
      </div>

      <div>
        <div className="flex items-center justify-between gap-4">
          <label
            htmlFor="password"
            className="text-sm font-semibold text-neutral-700 dark:text-neutral-200"
          >
            Contraseña
          </label>
          <button
            type="button"
            className="text-success-700 hover:text-success-900 focus-visible:outline-success-600 dark:text-success-400 dark:hover:text-success-200 text-xs font-semibold transition focus-visible:rounded focus-visible:outline-2 focus-visible:outline-offset-2"
            onClick={() => setRecoveryVisible((visible) => !visible)}
            aria-expanded={recoveryVisible}
            aria-controls="recovery-help"
          >
            ¿Olvidaste tu contraseña?
          </button>
        </div>
        <div className="relative mt-2">
          <LockKeyhole
            className="pointer-events-none absolute top-1/2 left-3.5 size-4.5 -translate-y-1/2 text-neutral-400"
            aria-hidden="true"
          />
          <input
            id="password"
            type={passwordVisible ? 'text' : 'password'}
            autoComplete="current-password"
            placeholder="Ingresa tu contraseña"
            aria-describedby={errors.password ? 'password-error' : undefined}
            aria-invalid={Boolean(errors.password)}
            className="focus:border-success-600 focus:ring-success-100 aria-invalid:border-danger-400 aria-invalid:focus:ring-danger-100 dark:focus:ring-success-900/40 h-12 w-full rounded-xl border border-neutral-200 bg-white pr-12 pl-11 text-sm text-neutral-900 shadow-sm transition outline-none placeholder:text-neutral-400 hover:border-neutral-300 focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:placeholder:text-neutral-500 dark:hover:border-neutral-600"
            {...register('password')}
          />
          <button
            type="button"
            onClick={() => setPasswordVisible((visible) => !visible)}
            className="focus-visible:outline-primary-600 absolute top-1/2 right-2.5 grid size-8 -translate-y-1/2 place-items-center rounded-lg text-neutral-400 transition hover:bg-neutral-100 hover:text-neutral-700 focus-visible:outline-2 focus-visible:outline-offset-1 dark:text-neutral-500 dark:hover:bg-neutral-800 dark:hover:text-neutral-100"
            aria-label={passwordVisible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
          >
            {passwordVisible ? <EyeOff className="size-4.5" /> : <Eye className="size-4.5" />}
          </button>
        </div>
        {errors.password ? (
          <p
            id="password-error"
            role="alert"
            className="text-danger-600 dark:text-danger-400 mt-1.5 text-xs font-medium"
          >
            {errors.password.message}
          </p>
        ) : null}
      </div>

      {recoveryVisible ? (
        <div
          id="recovery-help"
          role="status"
          className="border-secondary-100 bg-secondary-50 text-secondary-900 dark:border-secondary-800 dark:bg-secondary-900/30 dark:text-secondary-200 flex gap-3 rounded-xl border p-3.5 text-xs leading-5"
        >
          <CircleHelp
            className="text-secondary-600 dark:text-secondary-400 mt-0.5 size-4 shrink-0"
            aria-hidden="true"
          />
          Solicita el restablecimiento al administrador de tu organización. El enlace se enviará
          únicamente a tu correo corporativo.
        </div>
      ) : null}

      <label className="flex w-fit cursor-pointer items-center gap-2.5 text-sm text-neutral-600 dark:text-neutral-300">
        <input
          type="checkbox"
          className="text-success-700 focus:ring-success-600 size-4 rounded border-neutral-300 dark:border-neutral-600"
          {...register('remember')}
        />
        Recordar mi correo en este equipo
      </label>

      <Button
        type="submit"
        className="bg-success-600 hover:bg-success-700 focus-visible:outline-success-600 h-12 w-full text-[15px]"
        disabled={isSubmitting}
      >
        {isSubmitting ? (
          <>
            <LoaderCircle className="size-4.5 animate-spin" aria-hidden="true" />
            Verificando acceso
          </>
        ) : (
          <>
            Iniciar sesión
            <ArrowRight className="size-4.5" aria-hidden="true" />
          </>
        )}
      </Button>

      <div className="flex items-center gap-3">
        <div className="h-px flex-1 bg-neutral-200 dark:bg-neutral-700" />
        <span className="text-xs font-medium text-neutral-400 dark:text-neutral-500">
          o continúa con
        </span>
        <div className="h-px flex-1 bg-neutral-200 dark:bg-neutral-700" />
      </div>

      <div className="grid grid-cols-2 gap-3">
        <button
          type="button"
          className="flex h-11 items-center justify-center gap-2 rounded-xl border border-neutral-200 bg-white text-sm font-semibold text-neutral-700 transition hover:border-neutral-300 hover:bg-neutral-50 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-200 dark:hover:border-neutral-600 dark:hover:bg-neutral-800"
        >
          <QrCode className="size-4.5" aria-hidden="true" />
          Código QR
        </button>
        <button
          type="button"
          className="flex h-11 items-center justify-center gap-2 rounded-xl border border-neutral-200 bg-white text-sm font-semibold text-neutral-700 transition hover:border-neutral-300 hover:bg-neutral-50 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-200 dark:hover:border-neutral-600 dark:hover:bg-neutral-800"
        >
          <KeyRound className="size-4.5" aria-hidden="true" />
          PIN de caja
        </button>
      </div>

      <div className="flex items-start gap-2.5 rounded-xl bg-neutral-50 px-3.5 py-3 text-xs leading-5 text-neutral-500 dark:bg-neutral-800/50 dark:text-neutral-400">
        <ShieldCheck
          className="text-success-700 dark:text-success-400 mt-0.5 size-4 shrink-0"
          aria-hidden="true"
        />
        <span>
          Acceso protegido y auditado. Nunca compartas tus credenciales ni las almacenes en equipos
          públicos.
        </span>
      </div>

      <p className="flex items-center justify-center gap-1.5 text-center text-xs text-neutral-400">
        <CheckCircle2
          className="text-success-600 dark:text-success-400 size-3.5"
          aria-hidden="true"
        />
        Plataforma operativa · soporte interno habilitado
      </p>
    </form>
  );
}
