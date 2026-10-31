import {
  aOpcionales,
  numeroDeFormulario,
  numeroOpcional,
  textoDeFormulario,
  textoOpcional
} from './form-values';

describe('form-values', () => {
  it('textoOpcional recorta y convierte vacios en undefined', () => {
    expect(textoOpcional('  hola ')).toBe('hola');
    expect(textoOpcional('   ')).toBeUndefined();
    expect(textoOpcional(undefined)).toBeUndefined();
  });

  it('numeroOpcional convierte cadenas numericas y vacios', () => {
    expect(numeroOpcional(' 12.5 ')).toBe(12.5);
    expect(numeroOpcional('')).toBeUndefined();
    expect(numeroOpcional(undefined)).toBeUndefined();
  });

  it('textoDeFormulario sustituye nulos por cadena vacia', () => {
    expect(textoDeFormulario('abc')).toBe('abc');
    expect(textoDeFormulario(null)).toBe('');
    expect(textoDeFormulario(undefined)).toBe('');
  });

  it('numeroDeFormulario convierte numeros a texto y nulos a vacio', () => {
    expect(numeroDeFormulario(0)).toBe('0');
    expect(numeroDeFormulario(null)).toBe('');
    expect(numeroDeFormulario(undefined)).toBe('');
  });

  it('aOpcionales aplica textoOpcional a cada campo', () => {
    expect(aOpcionales({ a: ' x ', b: '' })).toEqual({ a: 'x', b: undefined });
  });
});
