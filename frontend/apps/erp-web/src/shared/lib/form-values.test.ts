import { emptyToUndefined, numeroOpcional, orEmpty } from './form-values';

describe('form-values', () => {
  it('emptyToUndefined recorta y convierte el vacío en undefined', () => {
    expect(emptyToUndefined('  ')).toBeUndefined();
    expect(emptyToUndefined(' F001 ')).toBe('F001');
  });

  it('orEmpty cambia null por texto vacío', () => {
    expect(orEmpty(null)).toBe('');
    expect(orEmpty('x')).toBe('x');
  });

  it('numeroOpcional convierte el texto recortado y deja undefined si está vacío', () => {
    expect(numeroOpcional('  ')).toBeUndefined();
    expect(numeroOpcional(' 3.81 ')).toBe(3.81);
    expect(numeroOpcional('-4.5')).toBe(-4.5);
  });
});
