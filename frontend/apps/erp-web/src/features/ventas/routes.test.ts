import { salesRoutes } from './routes';

describe('salesRoutes', () => {
  it('declara las rutas de ventas', () => {
    expect(salesRoutes.map(({ path }) => path)).toEqual(['ventas', 'ventas/:ventaId']);
  });

  it.each(salesRoutes.map((route) => [route.path, route] as const))(
    'carga la página de %s con lazy loading',
    async (_path, route) => {
      const loaded = await route.lazy();

      expect(typeof loaded.Component).toBe('function');
    }
  );
});
