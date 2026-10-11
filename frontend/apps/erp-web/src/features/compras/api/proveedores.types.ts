export const ESTADOS_PROVEEDOR = ['ACTIVO', 'SUSPENDIDO', 'BLOQUEADO'] as const;

export type EstadoProveedor = (typeof ESTADOS_PROVEEDOR)[number];

export type Proveedor = {
  id: string;
  tipoDocumento: string | null;
  numeroDocumento: string;
  razonSocial: string;
  nombreComercial: string | null;
  direccion: string | null;
  ubigeo: string | null;
  telefono: string | null;
  email: string | null;
  contactoNombre: string | null;
  contactoTelefono: string | null;
  contactoEmail: string | null;
  condicionPagoDefault: string | null;
  diasCreditoDefault: number;
  monedaDefault: string | null;
  esLaboratorio: boolean;
  esImportador: boolean;
  esDistribuidor: boolean;
  calificacion: string | null;
  estado: EstadoProveedor;
};

export type ProveedorPayload = {
  tipoDocumento?: string | undefined;
  numeroDocumento: string;
  razonSocial: string;
  nombreComercial?: string | undefined;
  direccion?: string | undefined;
  ubigeo?: string | undefined;
  telefono?: string | undefined;
  email?: string | undefined;
  contactoNombre?: string | undefined;
  contactoTelefono?: string | undefined;
  contactoEmail?: string | undefined;
  condicionPagoDefault?: string | undefined;
  diasCreditoDefault: number;
  monedaDefault: string;
  esLaboratorio: boolean;
  esImportador: boolean;
  esDistribuidor: boolean;
  calificacion?: string | undefined;
};
