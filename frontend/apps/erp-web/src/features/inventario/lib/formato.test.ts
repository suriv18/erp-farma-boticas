import { codigoCorto, formatearInstante } from './formato';

describe('formato', () => {
  it('codigoCorto devuelve los primeros 8 caracteres', () => {
    expect(codigoCorto('0123456789abcdef')).toBe('01234567');
  });

  it('formatearInstante muestra un guion cuando no hay fecha', () => {
    expect(formatearInstante(null)).toBe('—');
  });

  it('formatearInstante usa el formato regional es-PE', () => {
    const instante = '2026-10-02T15:30:00Z';
    expect(formatearInstante(instante)).toBe(new Date(instante).toLocaleString('es-PE'));
  });
});
