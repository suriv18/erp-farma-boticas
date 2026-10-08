import { purchasesRoutes } from './routes';

describe('purchasesRoutes', () => {
  it('declara las rutas del módulo de compras', () => {
    expect(purchasesRoutes.map(({ path }) => path)).toEqual([
      'compras',
      'compras/proveedores',
      'compras/proveedores/nuevo',
      'compras/proveedores/:proveedorId',
      'compras/ordenes',
      'compras/ordenes/nueva',
      'compras/ordenes/:ordenId',
      'compras/ordenes/:ordenId/recepcion'
    ]);
  });

  it.each(purchasesRoutes.map((route) => [route.path, route] as const))(
    'carga la página de %s con lazy loading',
    async (_path, route) => {
      const loaded = await route.lazy();

      expect(typeof loaded.Component).toBe('function');
    }
  );
});
