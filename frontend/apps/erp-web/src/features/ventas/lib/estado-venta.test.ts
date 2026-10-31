import { tonoEstadoVenta } from './estado-venta';

describe('tonoEstadoVenta', () => {
  it.each([
    ['CONFIRMADA', 'success'],
    ['ANULADA', 'danger'],
    ['PARCIALMENTE_DEVUELTA', 'warning'],
    ['DEVUELTA', 'neutral']
  ] as const)('asigna el tono de %s', (estado, tono) => {
    expect(tonoEstadoVenta(estado)).toBe(tono);
  });
});
