import type { EstadoEmpresa } from '../api/empresas.types';
import type { EstadoEstablecimiento } from '../api/establecimientos.types';

const EMPRESA_SIN_ALTAS: Partial<Record<EstadoEmpresa, string>> = {
  SUSPENDIDO: 'La empresa está suspendida; no admite establecimientos nuevos.',
  BLOQUEADO: 'La empresa está bloqueada; no admite establecimientos nuevos.'
};

const ESTABLECIMIENTO_SIN_ALTAS: Partial<Record<EstadoEstablecimiento, string>> = {
  SUSPENDIDO: 'El establecimiento está suspendido; no admite almacenes ni terminales POS nuevos.',
  CLAUSURADO: 'El establecimiento está clausurado; no admite almacenes ni terminales POS nuevos.'
};

export const motivoSinAltasEmpresa = (estado: EstadoEmpresa): string | null =>
  EMPRESA_SIN_ALTAS[estado] ?? null;

export const motivoSinAltasEstablecimiento = (estado: EstadoEstablecimiento): string | null =>
  ESTABLECIMIENTO_SIN_ALTAS[estado] ?? null;
