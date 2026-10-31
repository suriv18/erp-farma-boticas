import { ApiError } from '@boticas/api-client';
import { useState } from 'react';
import { Navigate } from 'react-router';
import { ChangePasswordForm } from '../components/ChangePasswordForm';
import { useAuthSession } from '../model/useAuthSession';
import type { ChangePasswordValues } from '../schemas/change-password.schema';

const REJECTED_ERROR = 'No se pudo cambiar la contraseña. Verifica la contraseña actual y que la nueva cumpla la política.';
const UNEXPECTED_ERROR = 'No se pudo cambiar la contraseña. Intenta nuevamente.';

export function ChangePasswordPage() {
  const { status, passwordChangeRequired, changePassword } = useAuthSession();
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [changed, setChanged] = useState(false);

  async function handleSubmit({ currentPassword, newPassword }: ChangePasswordValues) {
    setSubmitError(null);
    setChanged(true);
    try {
      await changePassword({ currentPassword, newPassword });
    } catch (error) {
      setChanged(false);
      setSubmitError(error instanceof ApiError ? REJECTED_ERROR : UNEXPECTED_ERROR);
    }
  }

  if (status === 'loading') {
    return null;
  }

  if (status === 'unauthenticated') {
    return <Navigate to="/login" replace state={changed ? { passwordChanged: true } : undefined} />;
  }

  if (!passwordChangeRequired) {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <main className="grid min-h-screen place-items-center bg-[#f4f7f5] px-5 py-10 dark:bg-neutral-950">
      <div className="w-full max-w-md">
        <h1 className="text-3xl font-black tracking-[-0.03em] text-neutral-950 dark:text-white">
          Cambia tu contraseña
        </h1>
        <p className="mt-3 text-sm leading-6 text-neutral-500 dark:text-neutral-400">
          Tu cuenta requiere una contraseña nueva antes de continuar. Al terminar deberás iniciar sesión otra vez.
        </p>
        <ChangePasswordForm onSubmit={handleSubmit} submitError={submitError} />
      </div>
    </main>
  );
}
