import { finDelDia, inicioDelDia } from './fechas';

describe('límites del día', () => {
  it('inicioDelDia es la medianoche local del día expresada como instante UTC', () => {
    expect(inicioDelDia('2026-10-03')).toBe(new Date(2026, 9, 3, 0, 0, 0, 0).toISOString());
  });

  it('finDelDia es el último milisegundo local del día expresado como instante UTC', () => {
    expect(finDelDia('2026-10-03')).toBe(new Date(2026, 9, 3, 23, 59, 59, 999).toISOString());
  });
});
