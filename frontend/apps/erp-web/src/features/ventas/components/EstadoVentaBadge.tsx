import { Badge } from '@boticas/ui-web';
import type { EstadoVenta } from '../api/ventas.types';
import { tonoEstadoVenta } from '../lib/estado-venta';

export function EstadoVentaBadge({ estado }: { estado: EstadoVenta }) {
  return <Badge tone={tonoEstadoVenta(estado)}>{estado}</Badge>;
}
