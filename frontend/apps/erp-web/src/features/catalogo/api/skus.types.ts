export const TIPOS_SKU = ['REGULADO', 'NO_REGULADO'] as const;
export const ESTADOS_SKU = ['ACTIVO', 'INACTIVO', 'BLOQUEADO', 'DESCONTINUADO'] as const;

export type TipoSku = (typeof TIPOS_SKU)[number];

export type CodigoBarraSku = {
  codigoBarra: string;
  tipoCodigo: string | null;
  esPrincipal: boolean;
  vigenteDesde: string | null;
  vigenteHasta: string | null;
  estado: string;
};

export type SkuResumen = {
  id: string;
  codigoInterno: string;
  descripcionComercial: string;
  tipoSku: string;
  estado: string;
  unidadVentaCodigo: string | null;
  permiteVentaFraccion: boolean;
  precioVentaReferencia: number | null;
};

export type Sku = {
  id: string;
  tenantId: string;
  productoReguladoId: string | null;
  categoriaId: string | null;
  marcaId: string | null;
  tipoSku: string;
  codigoInterno: string;
  descripcionComercial: string;
  nombreCorto: string | null;
  presentacionComercial: string | null;
  unidadVentaCodigo: string | null;
  contenido: number | null;
  unidadContenidoCodigo: string | null;
  pesoGramos: number | null;
  altoCm: number | null;
  anchoCm: number | null;
  largoCm: number | null;
  permiteVentaFraccion: boolean;
  factorFraccion: number | null;
  unidadFraccionCodigo: string | null;
  requiereLote: boolean;
  requiereVencimiento: boolean;
  afectoIgv: boolean;
  stockMinimoDefault: number;
  stockMaximoDefault: number | null;
  precioVentaReferencia: number | null;
  imagenUri: string | null;
  codigosBarra: CodigoBarraSku[];
  estado: string;
  createdBy: string;
  createdAt: string;
  updatedBy: string | null;
  updatedAt: string | null;
};

export type SkuPayload = {
  tipoSku: string;
  codigoInterno: string;
  descripcionComercial: string;
  productoReguladoId?: string | undefined;
  categoriaId?: string | undefined;
  marcaId?: string | undefined;
  nombreCorto?: string | undefined;
  presentacionComercial?: string | undefined;
  unidadVentaCodigo?: string | undefined;
  contenido?: number | undefined;
  unidadContenidoCodigo?: string | undefined;
  pesoGramos?: number | undefined;
  altoCm?: number | undefined;
  anchoCm?: number | undefined;
  largoCm?: number | undefined;
  permiteVentaFraccion: boolean;
  factorFraccion?: number | undefined;
  unidadFraccionCodigo?: string | undefined;
  requiereLote: boolean;
  requiereVencimiento: boolean;
  afectoIgv: boolean;
  stockMinimoDefault: number;
  stockMaximoDefault?: number | undefined;
  precioVentaReferencia?: number | undefined;
  imagenUri?: string | undefined;
};

export type AgregarCodigoBarraPayload = {
  codigoBarra: string;
  tipoCodigo?: string | undefined;
  vigenteDesde?: string | undefined;
  vigenteHasta?: string | undefined;
};
