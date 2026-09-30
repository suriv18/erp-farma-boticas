import type { EstadoEmpresa } from '../api/empresas.types';
import type { EstadoEstablecimiento } from '../api/establecimientos.types';

export function motivoSinAltasEmpresa(estado: EstadoEmpresa): string | null {
  return estado === 'ACTIVO'
    ? null
    : `La empresa está ${estado}; no admite establecimientos nuevos.`;
}

export function motivoSinAltasEstablecimiento(estado: EstadoEstablecimiento): string | null {
  return estado === 'SUSPENDIDO' || estado === 'CLAUSURADO'
    ? `El establecimiento está ${estado}; no admite almacenes ni terminales POS nuevos.`
    : null;
}
