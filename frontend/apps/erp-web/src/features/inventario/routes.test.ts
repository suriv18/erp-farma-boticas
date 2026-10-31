import { inventoryRoutes } from './routes';

describe('inventoryRoutes', () => {
  it('declara las rutas de la feature', () => {
    expect(inventoryRoutes.map(({ path }) => path)).toEqual([
      'inventario',
      'inventario/lotes/:loteId'
    ]);
  });

  it.each(inventoryRoutes.map((route) => [route.path, route] as const))(
    'carga la página de %s con lazy loading',
    async (_path, route) => {
      const loaded = await route.lazy();

      expect(typeof loaded.Component).toBe('function');
    }
  );
});
