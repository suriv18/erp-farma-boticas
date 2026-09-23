export type PaginaResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
};

export type Marca = {
  id: string;
  tenantId: string;
  codigo: string;
  nombre: string;
  descripcion: string | null;
  estado: string;
};

export type CrearMarcaPayload = {
  tenantId: string;
  codigo: string;
  nombre: string;
  descripcion?: string | undefined;
};

export type ActualizarMarcaPayload = {
  tenantId: string;
  codigo: string;
  nombre: string;
  descripcion?: string | undefined;
};
