import { principioActivoSchema } from './principio-activo.schema';

const valido = { codigoFuente: '', denominacion: 'Paracetamol', nombreNormalizado: '', fuente: '' };

describe('principioActivoSchema', () => {
  it('acepta un principio activo valido', () => {
    expect(principioActivoSchema.safeParse(valido).success).toBe(true);
  });

  it.each([
    ['codigoFuente', 'x'.repeat(81), 'El código fuente no debe exceder 80 caracteres.'],
    ['denominacion', 'a', 'La denominación debe tener al menos 2 caracteres.'],
    ['denominacion', 'x'.repeat(301), 'La denominación no debe exceder 300 caracteres.'],
    ['nombreNormalizado', 'x'.repeat(301), 'El nombre normalizado no debe exceder 300 caracteres.'],
    ['fuente', 'x'.repeat(301), 'La fuente no debe exceder 300 caracteres.']
  ])('rechaza %s invalido', (campo, valor, mensaje) => {
    const resultado = principioActivoSchema.safeParse({ ...valido, [campo]: valor });

    expect(resultado.success).toBe(false);
    expect(resultado.error?.issues[0]?.message).toBe(mensaje);
  });
});
