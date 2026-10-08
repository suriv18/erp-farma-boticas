import { empresaEdicionSchema, empresaSchema } from './empresa.schema';

const valid = {
  ruc: '20123456786',
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
    expect(messages({ ruc: '10123456781' })).toEqual([]);
  });

  it('rechaza un RUC con dígito verificador inválido', () => {
    expect(messages({ ruc: '20123456789' })).toEqual([
      'El RUC no es válido: el dígito verificador no coincide.'
    ]);
  });

  it('con formato inválido solo informa el formato, no el dígito verificador', () => {
    expect(messages({ ruc: '30123456789' })).toEqual([
      'El RUC debe tener 11 dígitos e iniciar con 10 o 20.'
    ]);
  });

  it.each([
    [{ ruc: '30123456789' }, 'El RUC debe tener 11 dígitos e iniciar con 10 o 20.'],
    [{ ruc: '2012345678' }, 'El RUC debe tener 11 dígitos e iniciar con 10 o 20.'],
    [{ razonSocial: 'A' }, 'La razón social debe tener al menos 2 caracteres.'],
    [{ razonSocial: 'x'.repeat(301) }, 'La razón social no debe exceder 300 caracteres.'],
    [{ nombreComercial: 'x'.repeat(301) }, 'El nombre comercial no debe exceder 300 caracteres.'],
    [{ direccionFiscal: 'x'.repeat(501) }, 'La dirección fiscal no debe exceder 500 caracteres.'],
    [{ ubigeoFiscal: '123' }, 'El ubigeo debe tener 6 dígitos.'],
    [
      { telefono: 'abc' },
      'El teléfono debe tener entre 6 y 15 dígitos y solo admite números, espacios, +, - y paréntesis.'
    ],
    [{ email: 'no-es-correo' }, 'El correo no es válido.'],
    [{ sitioWeb: 'x' }, 'El sitio web debe ser una URL que empiece con http:// o https://.'],
    [{ sitioWeb: `https://${'x'.repeat(300)}.pe` }, 'El sitio web no debe exceder 300 caracteres.'],
    [{ monedaFuncional: 'PE' }, 'La moneda debe tener 3 caracteres (ISO 4217).'],
    [{ zonaHoraria: '' }, 'La zona horaria es obligatoria.'],
    [{ zonaHoraria: 'x'.repeat(81) }, 'La zona horaria no debe exceder 80 caracteres.']
  ])('rechaza %j con "%s"', (overrides, message) => {
    expect(messages(overrides)).toContain(message);
  });
});

describe('empresaEdicionSchema', () => {
  const editar = (overrides: Record<string, unknown>) => {
    const result = empresaEdicionSchema.safeParse({ ...valid, ...overrides });
    return result.success ? [] : result.error.issues.map((issue) => issue.message);
  };

  it('acepta un RUC con formato válido aunque el dígito verificador no coincida', () => {
    expect(editar({ ruc: '20123456789' })).toEqual([]);
  });

  it('rechaza un RUC con formato inválido', () => {
    expect(editar({ ruc: '30123456789' })).toEqual([
      'El RUC debe tener 11 dígitos e iniciar con 10 o 20.'
    ]);
  });
});
