export type MiPuestoAlmacen = {
  id: string;
  codigo: string;
  nombre: string;
  tipo: string;
  permiteVenta: boolean;
};

export type MiPuestoTerminal = {
  id: string;
  codigo: string;
  nombre: string;
  estado: string;
  operable: boolean;
};

export type MiPuestoEstablecimiento = {
  id: string;
  codigo: string;
  nombre: string;
  almacenes: MiPuestoAlmacen[];
  terminales: MiPuestoTerminal[];
};

export type MiPuesto = {
  establecimientos: MiPuestoEstablecimiento[];
};
