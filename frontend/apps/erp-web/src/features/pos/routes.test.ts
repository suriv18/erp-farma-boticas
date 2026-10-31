import { posRoutes } from './routes';

describe('posRoutes', () => {
  it('declara la ruta del punto de venta', () => {
    expect(posRoutes.map(({ path }) => path)).toEqual(['pos']);
  });

  it.each(posRoutes.map((route) => [route.path, route] as const))(
    'carga la página de %s con lazy loading',
    async (_path, route) => {
      const loaded = await route.lazy();

      expect(typeof loaded.Component).toBe('function');
    }
  );
});
