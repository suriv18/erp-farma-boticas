import type { Proveedor } from '../api/proveedores.types';

export const rolesProveedor = (
  proveedor: Pick<Proveedor, 'esLaboratorio' | 'esImportador' | 'esDistribuidor'>
): string[] =>
  [
    proveedor.esLaboratorio ? 'Laboratorio' : null,
    proveedor.esImportador ? 'Importador' : null,
    proveedor.esDistribuidor ? 'Distribuidor' : null
  ].filter((rol): rol is string => rol !== null);
