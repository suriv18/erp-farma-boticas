import { rolesProveedor } from './proveedor-roles';

describe('rolesProveedor', () => {
  it('lista los roles marcados en orden', () => {
    expect(
      rolesProveedor({ esLaboratorio: true, esImportador: true, esDistribuidor: true })
    ).toEqual(['Laboratorio', 'Importador', 'Distribuidor']);
    expect(
      rolesProveedor({ esLaboratorio: false, esImportador: true, esDistribuidor: false })
    ).toEqual(['Importador']);
  });

  it('devuelve una lista vacía cuando no hay roles', () => {
    expect(
      rolesProveedor({ esLaboratorio: false, esImportador: false, esDistribuidor: false })
    ).toEqual([]);
  });
});
