import { Link } from 'react-router';
import { Card } from '@boticas/ui-web';

export function NotFoundPage() {
  return (
    <Card className="mx-auto max-w-xl p-8 text-center">
      <p className="text-primary-700 dark:text-primary-400 text-sm font-bold">404</p>
      <h1 className="mt-2 text-2xl font-bold text-neutral-950 dark:text-white">
        Página no encontrada
      </h1>
      <p className="mt-2 text-sm text-neutral-500 dark:text-neutral-400">
        La ruta solicitada no existe o ya no está disponible.
      </p>
      <Link
        className="text-primary-700 hover:text-primary-900 dark:text-primary-400 dark:hover:text-primary-200 mt-6 inline-flex text-sm font-semibold"
        to="/dashboard"
      >
        Ir al resumen
      </Link>
    </Card>
  );
}
