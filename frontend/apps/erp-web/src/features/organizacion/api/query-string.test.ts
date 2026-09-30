import { buildQuery } from './query-string';

describe('buildQuery', () => {
  it('serializa cadenas y números', () => {
    expect(buildQuery({ tenantId: 't-1', page: 0, size: 20 })).toBe('tenantId=t-1&page=0&size=20');
  });

  it('omite valores undefined y cadenas vacías', () => {
    expect(buildQuery({ tenantId: 't-1', search: '', empresaId: undefined })).toBe('tenantId=t-1');
  });

  it('codifica caracteres especiales', () => {
    expect(buildQuery({ search: 'a b&c' })).toBe('search=a+b%26c');
  });
});
