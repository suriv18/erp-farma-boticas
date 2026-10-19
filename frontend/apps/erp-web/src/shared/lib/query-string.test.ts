import { buildQueryString, withQuery } from './query-string';

describe('query-string', () => {
  it('omite indefinidos y vacíos y convierte números y booleanos', () => {
    expect(buildQueryString({ a: 'x', b: undefined, c: '', d: 0, e: false })).toBe(
      'a=x&d=0&e=false'
    );
  });

  it('withQuery devuelve la ruta sin signo ? cuando no hay parámetros', () => {
    expect(withQuery('/ruta', { a: undefined, b: '' })).toBe('/ruta');
  });

  it('withQuery agrega la query a la ruta', () => {
    expect(withQuery('/ruta', { a: 1, b: 'dos' })).toBe('/ruta?a=1&b=dos');
  });
});
