import { esCodigoBarras } from './codigo-barras';

describe('esCodigoBarras', () => {
  it.each(['12345678', '7750001234567', '12345678901234'])('acepta %s', (texto) => {
    expect(esCodigoBarras(texto)).toBe(true);
  });

  it.each(['1234567', '123456789012345', '77500012345ab', '', 'paracetamol'])(
    'rechaza "%s"',
    (texto) => {
      expect(esCodigoBarras(texto)).toBe(false);
    }
  );
});
