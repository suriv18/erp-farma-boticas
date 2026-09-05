import { Link } from 'react-router';
import { Card } from '@boticas/ui-web';

export function NotFoundPage() {
  return (
    <Card className="mx-auto max-w-xl p-8 text-center">
      <p className="text-sm font-bold text-teal-700">404</p>
      <h1 className="mt-2 text-2xl font-bold text-slate-950">Página no encontrada</h1>
      <p className="mt-2 text-sm text-slate-500">
        La ruta solicitada no existe o ya no está disponible.
      </p>
      <Link
        className="mt-6 inline-flex text-sm font-semibold text-teal-700 hover:text-teal-900"
        to="/dashboard"
      >
        Ir al resumen
      </Link>
    </Card>
  );
}
