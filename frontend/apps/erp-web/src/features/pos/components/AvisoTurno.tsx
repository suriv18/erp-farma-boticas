import { Link } from 'react-router';
import { Card, buttonClassName } from '@boticas/ui-web';

export function AvisoTurno() {
  return (
    <Card className="border-warning-200 bg-warning-50 text-warning-900 dark:border-warning-800 dark:bg-warning-900/20 dark:text-warning-100 flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between">
      <p className="text-sm font-medium">No hay un turno abierto en esta terminal.</p>
      <Link to="/caja" className={buttonClassName('secondary', 'sm')}>
        Ir a Caja
      </Link>
    </Card>
  );
}
