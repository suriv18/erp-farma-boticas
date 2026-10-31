export type EstadoVenta = 'CONFIRMADA' | 'ANULADA' | 'PARCIALMENTE_DEVUELTA' | 'DEVUELTA';

export type LoteConsumido = {
  loteId: string;
  cantidad: number;
};

export type LineaVenta = {
  numeroLinea: number;
  skuId: string;
  descripcion: string;
  unidadVentaCodigo: string;
  cantidad: number;
  precioUnitario: number;
  totalLinea: number;
  lotes: LoteConsumido[];
};

export type Pago = {
  medioPago: string;
  monto: number;
  montoRecibido: number;
  vuelto: number;
};

export type Anulacion = {
  anuladaAt: string;
  anuladaPorId: string;
  motivo: string;
};

export type Venta = {
  id: string;
  numeroOperacion: string;
  terminalId: string;
  turnoId: string;
  establecimientoId: string;
  vendedorId: string;
  fechaVenta: string;
  moneda: string;
  subtotal: number;
  descuentoTotal: number;
  impuestoTotal: number;
  total: number;
  estado: EstadoVenta;
  lineas: LineaVenta[];
  pago: Pago;
  anulacion: Anulacion | null;
};

export type VentaResumen = {
  id: string;
  numeroOperacion: string;
  terminalId: string;
  fechaVenta: string;
  total: number;
  estado: EstadoVenta;
};

export type RegistrarVentaPayload = {
  terminalId: string;
  almacenId: string;
  lineas: { skuId: string; cantidad: number; precioUnitario: number }[];
  pago: { montoRecibido: number };
};
