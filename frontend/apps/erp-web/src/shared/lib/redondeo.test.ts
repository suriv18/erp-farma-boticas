import { redondear } from './redondeo';

describe('redondear', () => {
  it('redondea al número de decimales indicado', () => {
    expect(redondear(1.005, 2)).toBe(1.01);
    expect(redondear(0.9999, 2)).toBe(1);
    expect(redondear(2.5, 0)).toBe(3);
    expect(redondear(1.23456, 4)).toBe(1.2346);
  });

  it('redondea medios céntimos hacia arriba como HALF_UP', () => {
    expect(redondear(2.385, 2)).toBe(2.39);
    expect(redondear(13.25 * 0.18, 2)).toBe(2.39);
    expect(redondear(1 * 4.145, 2)).toBe(4.15);
    expect(redondear(8.415, 2)).toBe(8.42);
    expect(redondear(9.745, 2)).toBe(9.75);
  });

  it('redondea los negativos alejándose de cero', () => {
    expect(redondear(-2.385, 2)).toBe(-2.39);
    expect(redondear(-0.004, 2)).toBe(0);
  });
});
