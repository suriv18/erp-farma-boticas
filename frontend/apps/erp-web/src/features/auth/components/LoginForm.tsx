import { zodResolver } from '@hookform/resolvers/zod';
import {
  CheckCircle2,
  CircleHelp,
  Eye,
  EyeOff,
  LoaderCircle,
  LockKeyhole,
  Mail,
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
        <p role="alert" className="rounded-xl border border-danger-200 bg-danger-50 px-3.5 py-3 text-xs font-medium text-danger-700 dark:border-danger-800 dark:bg-danger-900/30 dark:text-danger-300">
          {submitError}
        </p>
      ) : null}

      <div>
        <label htmlFor="email" className="text-sm font-semibold text-neutral-700 dark:text-neutral-200">
          Correo corporativo
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
            placeholder="nombre@boticas.pe"
            aria-describedby={errors.email ? 'email-error' : undefined}
            aria-invalid={Boolean(errors.email)}
            className="h-12 w-full rounded-xl border border-neutral-200 bg-white pr-4 pl-11 text-sm text-neutral-900 shadow-sm transition outline-none placeholder:text-neutral-400 hover:border-neutral-300 focus:border-primary-600 focus:ring-4 focus:ring-primary-100 aria-invalid:border-danger-400 aria-invalid:focus:ring-danger-100 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:placeholder:text-neutral-500 dark:hover:border-neutral-600 dark:focus:ring-primary-900/40"
            {...register('email')}
          />
        </div>
        {errors.email ? (
          <p id="email-error" role="alert" className="mt-1.5 text-xs font-medium text-danger-600 dark:text-danger-400">
            {errors.email.message}
          </p>
        ) : null}
      </div>

      <div>
        <div className="flex items-center justify-between gap-4">
          <label htmlFor="password" className="text-sm font-semibold text-neutral-700 dark:text-neutral-200">
            Contraseña
          </label>
          <button
            type="button"
            className="text-xs font-semibold text-primary-700 transition hover:text-primary-900 focus-visible:rounded focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-600 dark:text-primary-400 dark:hover:text-primary-200"
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
            className="h-12 w-full rounded-xl border border-neutral-200 bg-white pr-12 pl-11 text-sm text-neutral-900 shadow-sm transition outline-none placeholder:text-neutral-400 hover:border-neutral-300 focus:border-primary-600 focus:ring-4 focus:ring-primary-100 aria-invalid:border-danger-400 aria-invalid:focus:ring-danger-100 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:placeholder:text-neutral-500 dark:hover:border-neutral-600 dark:focus:ring-primary-900/40"
            {...register('password')}
          />
          <button
            type="button"
            onClick={() => setPasswordVisible((visible) => !visible)}
            className="absolute top-1/2 right-2.5 grid size-8 -translate-y-1/2 place-items-center rounded-lg text-neutral-400 transition hover:bg-neutral-100 hover:text-neutral-700 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-primary-600 dark:text-neutral-500 dark:hover:bg-neutral-800 dark:hover:text-neutral-100"
            aria-label={passwordVisible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
          >
            {passwordVisible ? <EyeOff className="size-4.5" /> : <Eye className="size-4.5" />}
          </button>
        </div>
        {errors.password ? (
          <p id="password-error" role="alert" className="mt-1.5 text-xs font-medium text-danger-600 dark:text-danger-400">
            {errors.password.message}
          </p>
        ) : null}
      </div>

      {recoveryVisible ? (
        <div
          id="recovery-help"
          role="status"
          className="flex gap-3 rounded-xl border border-secondary-100 bg-secondary-50 p-3.5 text-xs leading-5 text-secondary-900 dark:border-secondary-800 dark:bg-secondary-900/30 dark:text-secondary-200"
        >
          <CircleHelp className="mt-0.5 size-4 shrink-0 text-secondary-600 dark:text-secondary-400" aria-hidden="true" />
          Solicita el restablecimiento al administrador de tu organización. El enlace se enviará
          únicamente a tu correo corporativo.
        </div>
      ) : null}

      <label className="flex w-fit cursor-pointer items-center gap-2.5 text-sm text-neutral-600 dark:text-neutral-300">
        <input
          type="checkbox"
          className="size-4 rounded border-neutral-300 text-primary-700 focus:ring-primary-600 dark:border-neutral-600"
          {...register('remember')}
        />
        Recordar mi correo en este equipo
      </label>

      <Button type="submit" className="h-12 w-full text-[15px]" disabled={isSubmitting}>
        {isSubmitting ? (
          <>
            <LoaderCircle className="size-4.5 animate-spin" aria-hidden="true" />
            Verificando acceso
          </>
        ) : (
          <>Iniciar Sesión</>
        )}
      </Button>

      <div className="flex items-start gap-2.5 rounded-xl bg-neutral-50 px-3.5 py-3 text-xs leading-5 text-neutral-500 dark:bg-neutral-800/50 dark:text-neutral-400">
        <ShieldCheck className="mt-0.5 size-4 shrink-0 text-primary-700 dark:text-primary-400" aria-hidden="true" />
        <span>
          Acceso protegido y auditado. Nunca compartas tus credenciales ni las almacenes en equipos
          públicos.
        </span>
      </div>

      <p className="flex items-center justify-center gap-1.5 text-center text-xs text-neutral-400">
        <CheckCircle2 className="size-3.5 text-success-600 dark:text-success-400" aria-hidden="true" />
        Plataforma operativa · soporte interno habilitado
      </p>
    </form>
  );
}
