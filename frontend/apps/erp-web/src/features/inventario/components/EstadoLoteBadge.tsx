import { Badge } from '@boticas/ui-web';
import type { EstadoLote } from '../api/inventario.types';
import { tonoEstadoLote } from '../lib/estado-lote';

export function EstadoLoteBadge({ estado }: { estado: EstadoLote }) {
  return <Badge tone={tonoEstadoLote(estado)}>{estado}</Badge>;
}
