export const ESTADOS_ESTABLECIMIENTO = [
  'ACTIVO',
  'SUSPENDIDO',
  'CLAUSURADO',
  'REMODELACION'
] as const;
export const TIPOS_ESTABLECIMIENTO = ['BOTICA'] as const;
export const PERFILES_OPERACION = ['ONLINE', 'STORE_EDGE'] as const;

export type EstadoEstablecimiento = (typeof ESTADOS_ESTABLECIMIENTO)[number];
export type TipoEstablecimiento = (typeof TIPOS_ESTABLECIMIENTO)[number];
export type PerfilOperacion = (typeof PERFILES_OPERACION)[number];

export type Establecimiento = {
  id: string;
  tenantId: string;
  empresaId: string;
  codigo: string;
  nombre: string;
  tipoEstablecimiento: TipoEstablecimiento;
  categoriaRegulatoriaCodigo: string | null;
  codigoAnexoSunat: string;
  codigoDigemid: string | null;
  direccion: string | null;
  ubigeo: string | null;
  referencia: string | null;
  latitud: number | null;
  longitud: number | null;
  telefono: string | null;
  email: string | null;
  esPrincipal: boolean;
  permiteVentaOnline: boolean;
  permiteDelivery: boolean;
  perfilOperacion: PerfilOperacion;
  zonaHoraria: string;
  estadoOperativo: EstadoEstablecimiento;
  createdAt: string;
  updatedAt: string | null;
};

export type ActualizarEstablecimientoPayload = {
  nombre: string;
  tipoEstablecimiento: TipoEstablecimiento;
  categoriaRegulatoriaCodigo?: string | undefined;
  codigoAnexoSunat: string;
  codigoDigemid?: string | undefined;
  direccion?: string | undefined;
  ubigeo?: string | undefined;
  referencia?: string | undefined;
  latitud?: number | undefined;
  longitud?: number | undefined;
  telefono?: string | undefined;
  email?: string | undefined;
  esPrincipal: boolean;
  permiteVentaOnline: boolean;
  permiteDelivery: boolean;
  perfilOperacion: PerfilOperacion;
  zonaHoraria: string;
};

export type CrearEstablecimientoPayload = ActualizarEstablecimientoPayload & {
  tenantId: string;
  empresaId: string;
  codigo: string;
};
