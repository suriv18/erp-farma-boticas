import { resolverPaginacion } from './paginacion';

describe('resolverPaginacion', () => {
  it('aplica los valores por defecto cuando no se especifican', () => {
    expect(resolverPaginacion({})).toEqual({ page: 0, size: 20 });
  });

  it('respeta page y size explícitos, incluido el cero', () => {
    expect(resolverPaginacion({ page: 0, size: 5 })).toEqual({ page: 0, size: 5 });
    expect(resolverPaginacion({ page: 3, size: 50 })).toEqual({ page: 3, size: 50 });
  });
});
