import { UMBRAL_VENCIMIENTO_PROXIMO_DIAS, diasParaVencer, nivelVencimiento } from './vencimiento';

const hoy = new Date(2026, 9, 2, 23, 30);

describe('vencimiento', () => {
  it('el umbral de vencimiento próximo es de 90 días', () => {
    expect(UMBRAL_VENCIMIENTO_PROXIMO_DIAS).toBe(90);
  });

  it.each([
    ['2026-10-02', 0],
    ['2026-10-01', -1],
    ['2026-10-03', 1],
    ['2026-12-31', 90],
    ['2027-01-01', 91]
  ])('diasParaVencer(%s) es %i', (fecha, dias) => {
    expect(diasParaVencer(fecha, hoy)).toBe(dias);
  });

  it.each([
    ['2026-10-01', 'vencido'],
    ['2026-10-02', 'proximo'],
    ['2026-12-31', 'proximo'],
    ['2027-01-01', 'vigente']
  ])('nivelVencimiento(%s) es %s', (fecha, nivel) => {
    expect(nivelVencimiento(fecha, hoy)).toBe(nivel);
  });
});
