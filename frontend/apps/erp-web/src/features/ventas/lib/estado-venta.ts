import type { EstadoVenta } from '../api/ventas.types';

const TONOS = {
  CONFIRMADA: 'success',
  ANULADA: 'danger',
  PARCIALMENTE_DEVUELTA: 'warning',
  DEVUELTA: 'neutral'
} as const satisfies Record<EstadoVenta, 'success' | 'danger' | 'warning' | 'neutral'>;

export const tonoEstadoVenta = (estado: EstadoVenta) => TONOS[estado];
