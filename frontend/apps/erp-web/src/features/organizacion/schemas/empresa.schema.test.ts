import { empresaSchema } from './empresa.schema';

const valid = {
  ruc: '20123456789',
  razonSocial: 'Boticas SAC',
  nombreComercial: '',
  direccionFiscal: '',
  ubigeoFiscal: '',
  telefono: '',
  email: '',
  sitioWeb: '',
  monedaFuncional: 'PEN',
  zonaHoraria: 'America/Lima',
  permiteVentaOnline: false
};

function messages(overrides: Record<string, unknown>) {
  const result = empresaSchema.safeParse({ ...valid, ...overrides });
  return result.success ? [] : result.error.issues.map((issue) => issue.message);
}

describe('empresaSchema', () => {
  it('acepta una empresa válida con los opcionales vacíos', () => {
    expect(empresaSchema.safeParse(valid).success).toBe(true);
  });

  it('acepta un RUC de persona natural que inicia con 10', () => {
    expect(messages({ ruc: '10123456789' })).toEqual([]);
  });

  it.each([
    [{ ruc: '30123456789' }, 'El RUC debe tener 11 dígitos e iniciar con 10 o 20.'],
    [{ ruc: '2012345678' }, 'El RUC debe tener 11 dígitos e iniciar con 10 o 20.'],
    [{ razonSocial: 'A' }, 'La razón social debe tener al menos 2 caracteres.'],
    [{ razonSocial: 'x'.repeat(301) }, 'La razón social no debe exceder 300 caracteres.'],
    [{ nombreComercial: 'x'.repeat(301) }, 'El nombre comercial no debe exceder 300 caracteres.'],
    [{ direccionFiscal: 'x'.repeat(501) }, 'La dirección fiscal no debe exceder 500 caracteres.'],
    [{ ubigeoFiscal: '123' }, 'El ubigeo debe tener 6 dígitos.'],
    [{ telefono: 'x'.repeat(41) }, 'El teléfono no debe exceder 40 caracteres.'],
    [{ email: 'no-es-correo' }, 'El correo no es válido.'],
    [{ sitioWeb: 'x'.repeat(301) }, 'El sitio web no debe exceder 300 caracteres.'],
    [{ monedaFuncional: 'PE' }, 'La moneda debe tener 3 caracteres (ISO 4217).'],
    [{ zonaHoraria: '' }, 'La zona horaria es obligatoria.'],
    [{ zonaHoraria: 'x'.repeat(81) }, 'La zona horaria no debe exceder 80 caracteres.']
  ])('rechaza %j con "%s"', (overrides, message) => {
    expect(messages(overrides)).toContain(message);
  });
});
