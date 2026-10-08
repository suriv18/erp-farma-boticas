import { Badge } from '@boticas/ui-web';
import type { EstadoOrden } from '../api/ordenes.types';
import { etiquetaEstadoOrden, tonoEstadoOrden } from '../lib/estado-orden';

export function EstadoOrdenBadge({ estado }: { estado: EstadoOrden }) {
  return <Badge tone={tonoEstadoOrden(estado)}>{etiquetaEstadoOrden(estado)}</Badge>;
}
