import { Link } from 'react-router';
import { Card } from '@boticas/ui-web';

export function AvisoTurno() {
  return (
    <Card className="space-y-2 p-4">
      <p>No hay un turno abierto en esta terminal.</p>
      <Link to="/caja">Ir a Caja</Link>
    </Card>
  );
}
