import { redondear } from './redondeo';

describe('redondear', () => {
  it('redondea al número de decimales indicado', () => {
    expect(redondear(1.005, 2)).toBe(1.01);
    expect(redondear(0.9999, 2)).toBe(1);
    expect(redondear(2.5, 0)).toBe(3);
    expect(redondear(1.23456, 4)).toBe(1.2346);
  });
});
