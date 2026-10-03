export const ESTADOS_EMPRESA = ['ACTIVO', 'SUSPENDIDO', 'BLOQUEADO'] as const;

export type EstadoEmpresa = (typeof ESTADOS_EMPRESA)[number];

export type Empresa = {
  id: string;
  tenantId: string;
  ruc: string;
  razonSocial: string;
  nombreComercial: string | null;
  direccionFiscal: string | null;
  ubigeoFiscal: string | null;
  telefono: string | null;
  email: string | null;
  sitioWeb: string | null;
  monedaFuncional: string;
  zonaHoraria: string;
  permiteVentaOnline: boolean;
  estado: EstadoEmpresa;
  createdAt: string;
  updatedAt: string | null;
};

export type ActualizarEmpresaPayload = {
  razonSocial: string;
  nombreComercial?: string | undefined;
  direccionFiscal?: string | undefined;
  ubigeoFiscal?: string | undefined;
  telefono?: string | undefined;
  email?: string | undefined;
  sitioWeb?: string | undefined;
  monedaFuncional: string;
  zonaHoraria: string;
  permiteVentaOnline: boolean;
};

export type CrearEmpresaPayload = ActualizarEmpresaPayload & {
  ruc: string;
};
