export const ESTADOS_LOTE = [
  'HABILITADO',
  'CUARENTENA',
  'BLOQUEADO',
  'INMOVILIZADO_RECALL',
  'VENCIDO',
  'BAJA_DESTRUIDO'
] as const;

export type EstadoLote = (typeof ESTADOS_LOTE)[number];

export const TIPOS_AJUSTE = ['AJUSTE_INGRESO', 'AJUSTE_SALIDA'] as const;

export type TipoAjuste = (typeof TIPOS_AJUSTE)[number];

export type Posicion = {
  id: string;
  establecimientoId: string;
  almacenId: string;
  skuId: string;
  loteId: string;
  numeroLote: string;
  fechaVencimiento: string;
  estadoLote: EstadoLote;
  estadoInventario: string;
  cantidadFisica: number;
  cantidadReservada: number;
  cantidadDisponible: number;
  vendible: boolean;
  version: number;
  ultimoMovimientoAt: string | null;
};

export type Lote = {
  id: string;
  skuId: string;
  numeroLote: string;
  fechaVencimiento: string;
  estado: EstadoLote;
  motivoEstado: string | null;
  bloqueadoAt: string | null;
  vendible: boolean;
};

export type Movimiento = {
  id: string;
  posicionId: string;
  loteId: string;
  tipo: string;
  naturaleza: string;
  cantidad: number;
  stockAnterior: number;
  stockPosterior: number;
  fechaNegocio: string;
};

export type RegistrarMovimientoPayload = {
  almacenId: string;
  skuId: string;
  tipo: TipoAjuste;
  cantidad: number;
  motivo: string;
  loteId?: string | undefined;
  numeroLote?: string | undefined;
  fechaVencimiento?: string | undefined;
};

export type BloquearLotePayload = {
  motivo: string;
};
