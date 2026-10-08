import {
  esCantidad,
  esCantidadNoNegativa,
  esMonto,
  esPrecio,
  esTolerancia
} from './numeros-compras';

describe('numeros-compras', () => {
  it('esCantidadNoNegativa acepta cero y hasta 4 decimales', () => {
    expect(esCantidadNoNegativa('0')).toBe(true);
    expect(esCantidadNoNegativa('1.2345')).toBe(true);
    expect(esCantidadNoNegativa('1.23456')).toBe(false);
    expect(esCantidadNoNegativa('-1')).toBe(false);
    expect(esCantidadNoNegativa('')).toBe(false);
  });

  it('esCantidad además exige un valor mayor que cero', () => {
    expect(esCantidad('0')).toBe(false);
    expect(esCantidad('0.0001')).toBe(true);
  });

  it('esPrecio acepta cero y hasta 6 decimales', () => {
    expect(esPrecio('0')).toBe(true);
    expect(esPrecio('1.123456')).toBe(true);
    expect(esPrecio('1.1234567')).toBe(false);
    expect(esPrecio('abc')).toBe(false);
  });

  it('esMonto acepta hasta 2 decimales', () => {
    expect(esMonto('1.23')).toBe(true);
    expect(esMonto('1.234')).toBe(false);
  });

  it('esTolerancia acepta de 0 a 100 con hasta 4 decimales', () => {
    expect(esTolerancia('100')).toBe(true);
    expect(esTolerancia('100.0001')).toBe(false);
    expect(esTolerancia('1.00001')).toBe(false);
  });
});
