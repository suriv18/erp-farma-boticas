import type { EstadoEmpresa } from '../api/empresas.types';
import type { EstadoEstablecimiento } from '../api/establecimientos.types';

export const CONSECUENCIAS_ESTADO_EMPRESA: Partial<Record<EstadoEmpresa, string>> = {
  SUSPENDIDO:
    'Mientras esté suspendida, la empresa no admite establecimientos nuevos. Lo ya registrado no cambia de estado.',
  BLOQUEADO:
    'Mientras esté bloqueada, la empresa no admite establecimientos nuevos. Lo ya registrado no cambia de estado.'
};

export const CONSECUENCIAS_ESTADO_ESTABLECIMIENTO: Partial<Record<EstadoEstablecimiento, string>> =
  {
    SUSPENDIDO:
      'Mientras esté suspendido, el establecimiento no admite almacenes ni terminales POS nuevos. Lo ya registrado no cambia de estado.',
    CLAUSURADO:
      'Mientras esté clausurado, el establecimiento no admite almacenes ni terminales POS nuevos. Lo ya registrado no cambia de estado.'
  };
