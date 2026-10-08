import { RUC_FORMATO, tieneDigitoVerificadorValido } from './ruc';

describe('ruc', () => {
  it.each(['20123456786', '10123456781', '20000000010', '20000000061'])(
    'acepta el RUC %s con dígito verificador correcto',
    (ruc) => {
      expect(tieneDigitoVerificadorValido(ruc)).toBe(true);
    }
  );

  it.each(['20123456789', '20123456780', '10123456789'])(
    'rechaza el RUC %s con dígito verificador incorrecto',
    (ruc) => {
      expect(tieneDigitoVerificadorValido(ruc)).toBe(false);
    }
  );

  it('RUC_FORMATO exige 11 dígitos que inicien con 10 o 20', () => {
    expect(RUC_FORMATO.test('20123456786')).toBe(true);
    expect(RUC_FORMATO.test('30123456786')).toBe(false);
    expect(RUC_FORMATO.test('2012345678')).toBe(false);
  });
});
