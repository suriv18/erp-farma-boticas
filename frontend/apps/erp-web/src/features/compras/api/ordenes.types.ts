export const ESTADOS_ORDEN = [
  'BORRADOR',
  'EN_APROBACION',
  'APROBADA',
  'EMITIDA',
  'PARCIALMENTE_RECIBIDA',
  'RECIBIDA',
  'CANCELADA',
  'CERRADA'
] as const;

export type EstadoOrden = (typeof ESTADOS_ORDEN)[number];

export type OrdenResumen = {
  id: string;
  numero: string;
  proveedorId: string;
  proveedorRazonSocial: string;
  establecimientoDestinoId: string;
  fechaEmision: string;
  fechaEntregaEstimada: string | null;
  moneda: string;
  total: number;
  estado: EstadoOrden;
};

export type LineaOrden = {
  numeroLinea: number;
  skuId: string;
  descripcion: string;
  cantidad: number;
  unidadMedidaCodigo: string;
  precioUnitario: number;
  descuento: number;
  impuesto: number;
  totalLinea: number;
  toleranciaExcesoPct: number | null;
  toleranciaDefectoPct: number | null;
  cantidadRecibida: number;
  cantidadPendiente: number;
};

export type Orden = {
  id: string;
  numero: string;
  proveedorId: string;
  establecimientoDestinoId: string;
  fechaEmision: string;
  fechaEntregaEstimada: string | null;
  moneda: string;
  tipoCambio: number | null;
  condicionPago: string | null;
  diasCredito: number;
  subtotal: number;
  descuentoTotal: number;
  impuestoTotal: number;
  total: number;
  estado: EstadoOrden;
  observacion: string | null;
  aprobadoAt: string | null;
  lineas: LineaOrden[];
};

export type AnularOrdenPayload = { motivo: string };
