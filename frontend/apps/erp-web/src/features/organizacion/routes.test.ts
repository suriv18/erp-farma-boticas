import { organizationRoutes } from './routes';

describe('organizationRoutes', () => {
  it('declara las cuatro rutas de la feature', () => {
    expect(organizationRoutes.map(({ path }) => path)).toEqual([
      'organizacion',
      'organizacion/empresas',
      'organizacion/empresas/:empresaId',
      'organizacion/establecimientos/:establecimientoId'
    ]);
  });

  it.each(organizationRoutes.map((route) => [route.path, route] as const))(
    'carga la página de %s con lazy loading',
    async (_path, route) => {
      const loaded = await route.lazy();

      expect(typeof loaded.Component).toBe('function');
    }
  );
});
