import { emptyToUndefined, toNumberOrUndefined } from './form-values';

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
});
