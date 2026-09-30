import { establecimientoSchema } from './establecimiento.schema';

const valid = {
  codigo: 'EST001',
  nombre: 'Botica Central',
  tipoEstablecimiento: 'BOTICA',
  categoriaRegulatoriaCodigo: '',
  codigoAnexoSunat: '0001',
  codigoDigemid: '',
  direccion: '',
  ubigeo: '',
  referencia: '',
  latitud: '',
  longitud: '',
  telefono: '',
  email: '',
  esPrincipal: true,
  permiteVentaOnline: false,
  permiteDelivery: false,
  perfilOperacion: 'ONLINE',
  zonaHoraria: 'America/Lima'
};

function messages(overrides: Record<string, unknown>) {
  const result = establecimientoSchema.safeParse({ ...valid, ...overrides });
  return result.success ? [] : result.error.issues.map((issue) => issue.message);
}

describe('establecimientoSchema', () => {
  it('acepta un establecimiento válido con los opcionales vacíos', () => {
    expect(establecimientoSchema.safeParse(valid).success).toBe(true);
  });

  it('acepta coordenadas dentro de rango y el perfil STORE_EDGE', () => {
    expect(
      messages({ latitud: '-12.0464', longitud: '-77.0428', perfilOperacion: 'STORE_EDGE' })
    ).toEqual([]);
  });

  it.each([
    [{ codigo: '' }, 'El código es obligatorio.'],
    [{ codigo: 'x'.repeat(41) }, 'El código no debe exceder 40 caracteres.'],
    [{ nombre: 'A' }, 'El nombre debe tener al menos 2 caracteres.'],
    [{ nombre: 'x'.repeat(251) }, 'El nombre no debe exceder 250 caracteres.'],
    [{ tipoEstablecimiento: 'FARMACIA' }, 'Selecciona un tipo de establecimiento.'],
    [
      { categoriaRegulatoriaCodigo: 'x'.repeat(51) },
      'La categoría regulatoria no debe exceder 50 caracteres.'
    ],
    [{ codigoAnexoSunat: '12' }, 'El anexo SUNAT debe tener 4 dígitos.'],
    [{ codigoDigemid: 'x'.repeat(11) }, 'El código DIGEMID no debe exceder 10 caracteres.'],
    [{ direccion: 'x'.repeat(501) }, 'La dirección no debe exceder 500 caracteres.'],
    [{ ubigeo: '12' }, 'El ubigeo debe tener 6 dígitos.'],
    [{ referencia: 'x'.repeat(301) }, 'La referencia no debe exceder 300 caracteres.'],
    [{ latitud: '95' }, 'La latitud debe estar entre -90 y 90.'],
    [{ longitud: '181' }, 'La longitud debe estar entre -180 y 180.'],
    [{ telefono: 'x'.repeat(41) }, 'El teléfono no debe exceder 40 caracteres.'],
    [{ email: 'no-es-correo' }, 'El correo no es válido.'],
    [{ perfilOperacion: 'OFFLINE' }, 'Selecciona un perfil de operación.'],
    [{ zonaHoraria: '' }, 'La zona horaria es obligatoria.'],
    [{ zonaHoraria: 'x'.repeat(81) }, 'La zona horaria no debe exceder 80 caracteres.']
  ])('rechaza %j con "%s"', (overrides, message) => {
    expect(messages(overrides)).toContain(message);
  });
});
