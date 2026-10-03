import type { CorporateStructure } from '../../organizacion';

export type OpcionAlmacen = {
  id: string;
  nombre: string;
  establecimiento: string;
};

export type OpcionEstablecimiento = {
  id: string;
  nombre: string;
  almacenes: OpcionAlmacen[];
};

export function opcionesEstablecimientos(
  estructura: CorporateStructure | undefined
): OpcionEstablecimiento[] {
  return (estructura?.companies ?? []).flatMap((empresa) =>
    empresa.establishments.map((establecimiento) => ({
      id: establecimiento.id,
      nombre: establecimiento.name,
      almacenes: establecimiento.warehouses.map((almacen) => ({
        id: almacen.id,
        nombre: almacen.name,
        establecimiento: establecimiento.name
      }))
    }))
  );
}

export function almacenesDe(
  opciones: OpcionEstablecimiento[],
  establecimientoId: string | undefined
): OpcionAlmacen[] {
  const origen = establecimientoId
    ? opciones.filter(({ id }) => id === establecimientoId)
    : opciones;
  return origen.flatMap(({ almacenes }) => almacenes);
}
