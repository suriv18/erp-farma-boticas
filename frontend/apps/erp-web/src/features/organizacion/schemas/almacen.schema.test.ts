import { almacenSchema } from './almacen.schema';

const valid = {
  codigo: 'ALM001',
  nombre: 'Almacén Central',
  tipo: 'GENERAL',
  permiteLotes: true,
  permiteVencimiento: true,
  permiteVenta: true,
  permiteDespacho: true,
  controlTemperatura: false,
  temperaturaMinC: '',
  temperaturaMaxC: '',
  activo: true
};

function messages(overrides: Record<string, unknown>) {
  const result = almacenSchema.safeParse({ ...valid, ...overrides });
  return result.success ? [] : result.error.issues.map((issue) => issue.message);
}

describe('almacenSchema', () => {
  it('acepta un almacén válido sin temperaturas', () => {
    expect(almacenSchema.safeParse(valid).success).toBe(true);
  });

  it('acepta un almacén refrigerado con temperaturas', () => {
    expect(
      messages({
        tipo: 'REFRIGERADO',
        controlTemperatura: true,
        temperaturaMinC: '2',
        temperaturaMaxC: '8'
      })
    ).toEqual([]);
  });

  it.each([
    [{ codigo: '' }, 'El código es obligatorio.'],
    [{ codigo: 'x'.repeat(41) }, 'El código no debe exceder 40 caracteres.'],
    [{ nombre: 'A' }, 'El nombre debe tener al menos 2 caracteres.'],
    [{ nombre: 'x'.repeat(151) }, 'El nombre no debe exceder 150 caracteres.'],
    [{ tipo: 'OTRO' }, 'Selecciona un tipo de almacén.'],
    [{ temperaturaMinC: 'frio' }, 'La temperatura mínima debe ser un número.'],
    [{ temperaturaMaxC: 'calor' }, 'La temperatura máxima debe ser un número.']
  ])('rechaza %j con "%s"', (overrides, message) => {
    expect(messages(overrides)).toContain(message);
  });
});
