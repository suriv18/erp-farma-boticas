export type RubroComercial = {
  id: string;
  tenantId: string;
  codigo: string;
  nombre: string;
  descripcion: string | null;
  esFarmaceutico: boolean;
  orden: number;
  estado: string;
};

export type CrearRubroComercialPayload = {
  tenantId: string;
  codigo: string;
  nombre: string;
  descripcion?: string | undefined;
  esFarmaceutico: boolean;
  orden: number;
};

export type ActualizarRubroComercialPayload = {
  tenantId: string;
  codigo: string;
  nombre: string;
  descripcion?: string | undefined;
  esFarmaceutico: boolean;
  orden: number;
};
