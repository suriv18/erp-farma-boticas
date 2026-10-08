export type LineaRecepcion = {
  id: string;
  numeroLinea: number;
  numeroLineaOrden: number;
  skuId: string;
  numeroLote: string;
  fechaFabricacion: string | null;
  fechaVencimiento: string;
  cantidadRecibida: number;
  cantidadAceptada: number;
  cantidadRechazada: number;
  costoUnitario: number | null;
  decisionCalidad: string;
  motivoDecision: string | null;
  observacion: string | null;
  loteId: string | null;
};

export type Recepcion = {
  id: string;
  numero: string;
  ordenCompraId: string;
  proveedorId: string;
  establecimientoId: string;
  almacenId: string;
  documentoProveedorTipo: string | null;
  documentoProveedorSerie: string | null;
  documentoProveedorNumero: string | null;
  guiaRemisionRemitente: string | null;
  guiaRemisionTransportista: string | null;
  fechaRecepcion: string;
  temperaturaRecepcionC: number | null;
  humedadRelativaPct: number | null;
  estado: string;
  observacion: string | null;
  lineas: LineaRecepcion[];
};

export type ItemRecepcionPayload = {
  numeroLineaOrden: number;
  numeroLote: string;
  fechaFabricacion?: string | undefined;
  fechaVencimiento: string;
  cantidadRecibida: number;
  cantidadRechazada: number;
  motivoRechazo?: string | undefined;
  costoUnitario: number;
};

export type RegistrarRecepcionPayload = {
  ordenCompraId: string;
  almacenId: string;
  documentoProveedorTipo: string;
  documentoProveedorSerie?: string | undefined;
  documentoProveedorNumero?: string | undefined;
  guiaRemisionRemitente?: string | undefined;
  guiaRemisionTransportista?: string | undefined;
  temperaturaRecepcionC?: number | undefined;
  humedadRelativaPct?: number | undefined;
  observacion?: string | undefined;
  items: ItemRecepcionPayload[];
};
