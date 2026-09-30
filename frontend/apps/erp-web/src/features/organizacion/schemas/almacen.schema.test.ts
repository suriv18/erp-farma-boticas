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

const REQUIERE_AMBAS =
  'Indica la temperatura mínima y máxima cuando el almacén controla temperatura.';

function issues(overrides: Record<string, unknown>) {
  const result = almacenSchema.safeParse({ ...valid, ...overrides });
  return result.success
    ? []
    : result.error.issues.map((issue) => `${issue.path.join('.')}: ${issue.message}`);
}

function messages(overrides: Record<string, unknown>) {
  return issues(overrides).map((issue) => issue.slice(issue.indexOf(': ') + 2));
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

  it('rechaza un almacén refrigerado que no controla temperatura', () => {
    expect(issues({ tipo: 'REFRIGERADO', controlTemperatura: false })).toContain(
      'controlTemperatura: Un almacén refrigerado debe controlar temperatura.'
    );
  });

  it('exige ambas temperaturas cuando controla temperatura y marca el campo que falta', () => {
    expect(issues({ controlTemperatura: true, temperaturaMinC: '', temperaturaMaxC: '8' })).toEqual(
      [`temperaturaMinC: ${REQUIERE_AMBAS}`]
    );
    expect(issues({ controlTemperatura: true, temperaturaMinC: '2', temperaturaMaxC: '' })).toEqual(
      [`temperaturaMaxC: ${REQUIERE_AMBAS}`]
    );
    expect(issues({ controlTemperatura: true, temperaturaMinC: '', temperaturaMaxC: '' })).toEqual([
      `temperaturaMinC: ${REQUIERE_AMBAS}`,
      `temperaturaMaxC: ${REQUIERE_AMBAS}`
    ]);
  });

  it('rechaza una temperatura mínima mayor que la máxima y acepta valores iguales', () => {
    expect(issues({ temperaturaMinC: '8', temperaturaMaxC: '2' })).toEqual([
      'temperaturaMinC: La temperatura mínima no puede ser mayor que la máxima.'
    ]);
    expect(issues({ temperaturaMinC: '5', temperaturaMaxC: '5' })).toEqual([]);
  });

  it('acepta una sola temperatura cuando no controla temperatura', () => {
    expect(issues({ temperaturaMinC: '', temperaturaMaxC: '8' })).toEqual([]);
    expect(issues({ temperaturaMinC: '2', temperaturaMaxC: '' })).toEqual([]);
  });
});
