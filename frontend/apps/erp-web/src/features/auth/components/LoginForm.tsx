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
};

export function LoginForm({ onAuthenticate }: LoginFormProps) {
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
      <div>
        <label htmlFor="email" className="text-sm font-semibold text-slate-700">
          Correo corporativo
        </label>
        <div className="relative mt-2">
          <Mail
            className="pointer-events-none absolute top-1/2 left-3.5 size-4.5 -translate-y-1/2 text-slate-400"
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
            className="h-12 w-full rounded-xl border border-slate-200 bg-white pr-4 pl-11 text-sm text-slate-900 shadow-sm transition outline-none placeholder:text-slate-400 hover:border-slate-300 focus:border-teal-600 focus:ring-4 focus:ring-teal-100 aria-invalid:border-rose-400 aria-invalid:focus:ring-rose-100"
            {...register('email')}
          />
        </div>
        {errors.email ? (
          <p id="email-error" role="alert" className="mt-1.5 text-xs font-medium text-rose-600">
            {errors.email.message}
          </p>
        ) : null}
      </div>

      <div>
        <div className="flex items-center justify-between gap-4">
          <label htmlFor="password" className="text-sm font-semibold text-slate-700">
            Contraseña
          </label>
          <button
            type="button"
            className="text-xs font-semibold text-teal-700 transition hover:text-teal-900 focus-visible:rounded focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-600"
            onClick={() => setRecoveryVisible((visible) => !visible)}
            aria-expanded={recoveryVisible}
            aria-controls="recovery-help"
          >
            ¿Olvidaste tu contraseña?
          </button>
        </div>
        <div className="relative mt-2">
          <LockKeyhole
            className="pointer-events-none absolute top-1/2 left-3.5 size-4.5 -translate-y-1/2 text-slate-400"
            aria-hidden="true"
          />
          <input
            id="password"
            type={passwordVisible ? 'text' : 'password'}
            autoComplete="current-password"
            placeholder="Ingresa tu contraseña"
            aria-describedby={errors.password ? 'password-error' : undefined}
            aria-invalid={Boolean(errors.password)}
            className="h-12 w-full rounded-xl border border-slate-200 bg-white pr-12 pl-11 text-sm text-slate-900 shadow-sm transition outline-none placeholder:text-slate-400 hover:border-slate-300 focus:border-teal-600 focus:ring-4 focus:ring-teal-100 aria-invalid:border-rose-400 aria-invalid:focus:ring-rose-100"
            {...register('password')}
          />
          <button
            type="button"
            onClick={() => setPasswordVisible((visible) => !visible)}
            className="absolute top-1/2 right-2.5 grid size-8 -translate-y-1/2 place-items-center rounded-lg text-slate-400 transition hover:bg-slate-100 hover:text-slate-700 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-teal-600"
            aria-label={passwordVisible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
          >
            {passwordVisible ? <EyeOff className="size-4.5" /> : <Eye className="size-4.5" />}
          </button>
        </div>
        {errors.password ? (
          <p id="password-error" role="alert" className="mt-1.5 text-xs font-medium text-rose-600">
            {errors.password.message}
          </p>
        ) : null}
      </div>

      {recoveryVisible ? (
        <div
          id="recovery-help"
          role="status"
          className="flex gap-3 rounded-xl border border-blue-100 bg-blue-50 p-3.5 text-xs leading-5 text-blue-900"
        >
          <CircleHelp className="mt-0.5 size-4 shrink-0 text-blue-600" aria-hidden="true" />
          Solicita el restablecimiento al administrador de tu organización. El enlace se enviará
          únicamente a tu correo corporativo.
        </div>
      ) : null}

      <label className="flex w-fit cursor-pointer items-center gap-2.5 text-sm text-slate-600">
        <input
          type="checkbox"
          className="size-4 rounded border-slate-300 text-teal-700 focus:ring-teal-600"
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

      <div className="flex items-start gap-2.5 rounded-xl bg-slate-50 px-3.5 py-3 text-xs leading-5 text-slate-500">
        <ShieldCheck className="mt-0.5 size-4 shrink-0 text-teal-700" aria-hidden="true" />
        <span>
          Acceso protegido y auditado. Nunca compartas tus credenciales ni las almacenes en equipos
          públicos.
        </span>
      </div>

      <p className="flex items-center justify-center gap-1.5 text-center text-xs text-slate-400">
        <CheckCircle2 className="size-3.5 text-emerald-600" aria-hidden="true" />
        Plataforma operativa · soporte interno habilitado
      </p>
    </form>
  );
}
