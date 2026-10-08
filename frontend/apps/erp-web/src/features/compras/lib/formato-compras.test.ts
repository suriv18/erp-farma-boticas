import { formatoMoneda } from '../../../shared/lib/format';
import { formatoFecha, formatoImporte } from './formato-compras';

describe('formato-compras', () => {
  it('formatea el importe en la moneda de la orden', () => {
    expect(formatoImporte(64.9, 'PEN')).toBe(formatoMoneda(64.9));
    expect(formatoImporte(10, 'USD')).toMatch(/10[.,]00/);
  });

  it('con una moneda inválida muestra el código y el importe', () => {
    expect(formatoImporte(10, 'ZZ')).toBe('ZZ 10.00');
  });

  it('formatea la fecha sin desfase de zona horaria', () => {
    expect(formatoFecha('2026-10-03')).toBe(new Date(2026, 9, 3).toLocaleDateString('es-PE'));
  });

  it('muestra un guion cuando no hay fecha', () => {
    expect(formatoFecha(null)).toBe('—');
  });
});
