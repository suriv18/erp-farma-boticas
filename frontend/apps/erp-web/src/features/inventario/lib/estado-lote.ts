import type { EstadoLote } from '../api/inventario.types';

type Tono = 'success' | 'warning' | 'danger' | 'neutral';

const TONOS: Record<EstadoLote, Tono> = {
  HABILITADO: 'success',
  CUARENTENA: 'warning',
  BLOQUEADO: 'danger',
  INMOVILIZADO_RECALL: 'danger',
  VENCIDO: 'neutral',
  BAJA_DESTRUIDO: 'neutral'
};

const BLOQUEABLES: readonly EstadoLote[] = ['HABILITADO', 'CUARENTENA'];

export const tonoEstadoLote = (estado: EstadoLote): Tono => TONOS[estado];

export const puedeBloquear = (estado: EstadoLote): boolean => BLOQUEABLES.includes(estado);

export const puedeDesbloquear = (estado: EstadoLote): boolean => estado === 'BLOQUEADO';
