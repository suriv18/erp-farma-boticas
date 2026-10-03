export type { PaginaResponse } from '../../../shared/lib/pagina.types';

export type Marca = {
  id: string;
  tenantId: string;
  codigo: string;
  nombre: string;
  descripcion: string | null;
  estado: string;
};

export type CrearMarcaPayload = {
  codigo: string;
  nombre: string;
  descripcion?: string | undefined;
};

export type ActualizarMarcaPayload = {
  codigo: string;
  nombre: string;
  descripcion?: string | undefined;
};
