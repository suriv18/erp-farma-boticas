import { formatoFechaHora, formatoMoneda, valueOrDash, yesNo } from './format';

describe('format', () => {
  it('valueOrDash devuelve el valor cuando existe', () => {
    expect(valueOrDash('Av. Principal 100')).toBe('Av. Principal 100');
  });

  it('valueOrDash devuelve un guion para null y cadenas vacías', () => {
    expect(valueOrDash(null)).toBe('—');
    expect(valueOrDash('')).toBe('—');
  });

  it('yesNo traduce booleanos', () => {
    expect(yesNo(true)).toBe('Sí');
    expect(yesNo(false)).toBe('No');
  });
});

describe('formatoMoneda', () => {
  it('formatea en soles con dos decimales', () => {
    expect(formatoMoneda(1234.5).replace(/\s/g, ' ')).toBe('S/ 1,234.50');
    expect(formatoMoneda(0).replace(/\s/g, ' ')).toBe('S/ 0.00');
  });
});

describe('formatoFechaHora', () => {
  it('devuelve un guion cuando no hay instante', () => {
    expect(formatoFechaHora(null)).toBe('—');
  });

  it('formatea el instante con la configuración regional peruana', () => {
    expect(formatoFechaHora('2026-10-03T15:30:00Z')).toBe(
      new Date('2026-10-03T15:30:00Z').toLocaleString('es-PE')
    );
  });
});
