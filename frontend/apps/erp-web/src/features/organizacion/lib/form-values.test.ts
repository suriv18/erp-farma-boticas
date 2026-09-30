import {
  aMayusculasSinEspacios,
  emptyToUndefined,
  numberOrEmpty,
  orEmpty,
  toNumberOrUndefined
} from './form-values';

describe('form-values', () => {
  it('emptyToUndefined recorta y convierte vacíos en undefined', () => {
    expect(emptyToUndefined('  Botica  ')).toBe('Botica');
    expect(emptyToUndefined('')).toBeUndefined();
    expect(emptyToUndefined('   ')).toBeUndefined();
  });

  it('toNumberOrUndefined convierte texto numérico y vacíos', () => {
    expect(toNumberOrUndefined('-12.5')).toBe(-12.5);
    expect(toNumberOrUndefined(' 4 ')).toBe(4);
    expect(toNumberOrUndefined('')).toBeUndefined();
    expect(toNumberOrUndefined('  ')).toBeUndefined();
  });

  it('orEmpty y numberOrEmpty convierten null en cadena vacía', () => {
    expect(orEmpty('Av. 1')).toBe('Av. 1');
    expect(orEmpty(null)).toBe('');
    expect(numberOrEmpty(-12.5)).toBe('-12.5');
    expect(numberOrEmpty(0)).toBe('0');
    expect(numberOrEmpty(null)).toBe('');
  });
});

describe('aMayusculasSinEspacios', () => {
  it('convierte a mayúscula y elimina todos los espacios', () => {
    expect(aMayusculasSinEspacios(' b0 01 ')).toBe('B001');
    expect(aMayusculasSinEspacios('')).toBe('');
  });
});
