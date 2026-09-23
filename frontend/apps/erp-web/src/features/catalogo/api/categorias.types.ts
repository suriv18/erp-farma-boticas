export type CategoriaProducto = {
  id: string;
  tenantId: string;
  categoriaPadreId: string | null;
  codigo: string;
  nombre: string;
  descripcion: string | null;
  nivel: number;
  orden: number;
  estado: string;
};

export type CrearCategoriaPayload = {
  tenantId: string;
  categoriaPadreId?: string | undefined;
  codigo: string;
  nombre: string;
  descripcion?: string | undefined;
  nivel: number;
  orden: number;
};

export type ActualizarCategoriaPayload = {
  tenantId: string;
  categoriaPadreId?: string | undefined;
  codigo: string;
  nombre: string;
  descripcion?: string | undefined;
  nivel: number;
  orden: number;
};
