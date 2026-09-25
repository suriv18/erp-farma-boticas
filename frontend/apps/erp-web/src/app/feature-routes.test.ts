import { describe, expect, it } from 'vitest';
import { erpFeatureRoutes, publicFeatureRoutes } from './feature-routes';

describe('featureRoutes', () => {
  it('ensambla los módulos internos una sola vez y mediante lazy loading', () => {
    const paths = erpFeatureRoutes.map(({ path }) => path);

    expect(paths).toEqual([
      'dashboard',
      'catalogo',
      'catalogo/marcas',
      'catalogo/categorias',
      'catalogo/rubros-comerciales',
      'catalogo/condiciones-venta',
      'catalogo/formas-farmaceuticas',
      'catalogo/vias-administracion',
      'catalogo/unidades-medida',
      'catalogo/clasificaciones-controladas',
      'inventario',
      'compras',
      'ventas',
      'pos',
      'caja',
      'clientes',
      'seguridad',
      'seguridad/permisos',
      'seguridad/usuarios',
      'seguridad/usuarios/:userId',
      'seguridad/roles',
      'seguridad/roles/:roleId',
      'organizacion',
      'perfil',
      'perfil/seguridad',
      'perfil/configuraciones',
      'perfil/sucursal',
      'perfil/ayuda'
    ]);
    expect(new Set(paths).size).toBe(paths.length);
    expect(erpFeatureRoutes.every(({ lazy }) => typeof lazy === 'function')).toBe(true);
  });

  it('mantiene autenticación fuera del shell privado', () => {
    expect(publicFeatureRoutes.map(({ path }) => path)).toEqual(['/login']);
    expect(publicFeatureRoutes.every(({ lazy }) => typeof lazy === 'function')).toBe(true);
  });
});
