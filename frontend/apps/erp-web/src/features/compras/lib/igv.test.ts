import { TASA_IGV, impuestoSugerido } from './igv';

describe('igv', () => {
  it('la tasa sugerida es 18%', () => {
    expect(TASA_IGV).toBe(0.18);
  });

  it('sugiere el 18% de la base redondeado a dos decimales para productos afectos', () => {
    expect(impuestoSugerido(5.5, true)).toBe(0.99);
    expect(impuestoSugerido(10.01, true)).toBe(1.8);
    expect(impuestoSugerido(13.25, true)).toBe(2.39);
  });

  it('no sugiere impuesto para productos no afectos', () => {
    expect(impuestoSugerido(100, false)).toBe(0);
  });
});
