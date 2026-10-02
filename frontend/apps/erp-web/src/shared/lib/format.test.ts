import { valueOrDash, yesNo } from './format';

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
