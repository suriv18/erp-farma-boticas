import type { EstadoOrden } from '../api/ordenes.types';

type Tono = 'success' | 'warning' | 'danger' | 'neutral';

const ETIQUETAS = {
  BORRADOR: 'Borrador',
  EN_APROBACION: 'En aprobación',
  APROBADA: 'Aprobada',
  EMITIDA: 'Emitida',
  PARCIALMENTE_RECIBIDA: 'Parcialmente recibida',
  RECIBIDA: 'Recibida',
  CANCELADA: 'Cancelada',
  CERRADA: 'Cerrada'
} as const satisfies Record<EstadoOrden, string>;

const TONOS = {
  BORRADOR: 'neutral',
  EN_APROBACION: 'warning',
  APROBADA: 'success',
  EMITIDA: 'success',
  PARCIALMENTE_RECIBIDA: 'warning',
  RECIBIDA: 'success',
  CANCELADA: 'danger',
  CERRADA: 'neutral'
} as const satisfies Record<EstadoOrden, Tono>;

const ANULABLES: readonly EstadoOrden[] = ['BORRADOR', 'EN_APROBACION', 'APROBADA', 'EMITIDA'];

export const etiquetaEstadoOrden = (estado: EstadoOrden): string => ETIQUETAS[estado];

export const tonoEstadoOrden = (estado: EstadoOrden): Tono => TONOS[estado];

export const puedeAprobar = (estado: EstadoOrden): boolean =>
  estado === 'BORRADOR' || estado === 'EN_APROBACION';

export const puedeEmitir = (estado: EstadoOrden): boolean => estado === 'APROBADA';

export const puedeAnular = (estado: EstadoOrden): boolean => ANULABLES.includes(estado);
