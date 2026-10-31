export const ESTADOS_REGULATORIOS = [
  'VIGENTE',
  'VENCIDO',
  'SUSPENDIDO',
  'CANCELADO',
  'POR_VALIDAR'
] as const;

export type PrincipioActivoAsociado = {
  principioActivoId: string;
  concentracionTexto: string | null;
  cantidad: number | null;
  unidadMedidaCodigo: string | null;
  esPrincipal: boolean;
  orden: number;
};

export type ProductoReguladoResumen = {
  id: string;
  denominacion: string;
  condicionVentaCodigo: string | null;
  estadoRegulatorio: string;
};

export type ProductoRegulado = {
  id: string;
  tipoProducto: string;
  rubroCodigo: string | null;
  tipoRegistro: string | null;
  numeroRegistro: string | null;
  denominacion: string;
  concentracionTexto: string | null;
  presentacionRegulatoria: string | null;
  formaFarmaceuticaCodigo: string | null;
  viaAdministracionCodigo: string | null;
  unidadMedidaCodigo: string | null;
  condicionVentaCodigo: string | null;
  clasificacionAtc: string | null;
  clasificacionControladaCodigo: string | null;
  tipoLiberacion: string | null;
  origenFabricacion: string | null;
  paisOrigen: string | null;
  subpartidaNacional: string | null;
  titularRegistro: string | null;
  fabricante: string | null;
  importador: string | null;
  establecimientoExpendio: string | null;
  vigenteDesde: string | null;
  vigenteHasta: string | null;
  fuente: string | null;
  versionFuente: string | null;
  principiosActivos: PrincipioActivoAsociado[];
  estadoRegulatorio: string;
  createdAt: string;
  updatedAt: string | null;
};

export type ProductoReguladoPayload = {
  tipoProducto: string;
  denominacion: string;
  rubroCodigo?: string | undefined;
  tipoRegistro?: string | undefined;
  numeroRegistro?: string | undefined;
  concentracionTexto?: string | undefined;
  presentacionRegulatoria?: string | undefined;
  formaFarmaceuticaCodigo?: string | undefined;
  viaAdministracionCodigo?: string | undefined;
  unidadMedidaCodigo?: string | undefined;
  condicionVentaCodigo?: string | undefined;
  clasificacionAtc?: string | undefined;
  clasificacionControladaCodigo?: string | undefined;
  tipoLiberacion?: string | undefined;
  origenFabricacion?: string | undefined;
  paisOrigen?: string | undefined;
  subpartidaNacional?: string | undefined;
  titularRegistro?: string | undefined;
  fabricante?: string | undefined;
  importador?: string | undefined;
  establecimientoExpendio?: string | undefined;
  vigenteDesde?: string | undefined;
  vigenteHasta?: string | undefined;
  fuente?: string | undefined;
  versionFuente?: string | undefined;
};

export type AsociarPrincipioActivoPayload = {
  principioActivoId: string;
  concentracionTexto?: string | undefined;
  cantidad?: number | undefined;
  unidadMedidaCodigo?: string | undefined;
  esPrincipal: boolean;
  orden: number;
};
