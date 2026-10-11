import { cashRegisterRoutes } from './routes';

describe('cashRegisterRoutes', () => {
  it('declara la ruta de caja', () => {
    expect(cashRegisterRoutes.map(({ path }) => path)).toEqual(['caja']);
  });

  it.each(cashRegisterRoutes.map((route) => [route.path, route] as const))(
    'carga la página de %s con lazy loading',
    async (_path, route) => {
      const loaded = await route.lazy();

      expect(typeof loaded.Component).toBe('function');
    }
  );
});
