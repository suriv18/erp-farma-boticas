export const TIPOS_ALMACEN = [
  'VENTA',
  'GENERAL',
  'CUARENTENA',
  'REFRIGERADO',
  'PSICOTROPICO',
  'MERMA'
] as const;

export type TipoAlmacen = (typeof TIPOS_ALMACEN)[number];

export type Almacen = {
  id: string;
  tenantId: string;
  establecimientoId: string;
  codigo: string;
  nombre: string;
  tipo: TipoAlmacen;
  permiteLotes: boolean;
  permiteVencimiento: boolean;
  permiteVenta: boolean;
  permiteDespacho: boolean;
  controlTemperatura: boolean;
  temperaturaMinC: number | null;
  temperaturaMaxC: number | null;
  activo: boolean;
  createdAt: string;
  updatedAt: string | null;
};

type AlmacenDatos = {
  nombre: string;
  tipo: TipoAlmacen;
  permiteLotes: boolean;
  permiteVencimiento: boolean;
  permiteVenta: boolean;
  permiteDespacho: boolean;
  controlTemperatura: boolean;
  temperaturaMinC?: number | undefined;
  temperaturaMaxC?: number | undefined;
};

export type CrearAlmacenPayload = AlmacenDatos & {
  tenantId: string;
  establecimientoId: string;
  codigo: string;
};

export type ActualizarAlmacenPayload = AlmacenDatos & {
  activo: boolean;
};
