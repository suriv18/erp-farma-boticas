import type { ZodType } from 'zod';
import { correoOpcional, numeroOpcional, textoOpcional, ubigeoOpcional } from './campos';

function messages(schema: ZodType, value: unknown) {
  const result = schema.safeParse(value);
  return result.success ? [] : result.error.issues.map((issue) => issue.message);
}

describe('campos', () => {
  it('textoOpcional acepta vacío y rechaza textos largos con la etiqueta indicada', () => {
    const schema = textoOpcional(5, 'El teléfono');
    expect(messages(schema, '')).toEqual([]);
    expect(messages(schema, '12345')).toEqual([]);
    expect(messages(schema, '123456')).toEqual(['El teléfono no debe exceder 5 caracteres.']);
  });

  it('correoOpcional acepta vacío y correos válidos', () => {
    expect(messages(correoOpcional, '')).toEqual([]);
    expect(messages(correoOpcional, 'contacto@boticas.pe')).toEqual([]);
  });

  it('correoOpcional rechaza formatos inválidos y textos demasiado largos', () => {
    expect(messages(correoOpcional, 'no-es-correo')).toEqual(['El correo no es válido.']);
    expect(messages(correoOpcional, `${'a'.repeat(320)}@b.pe`)).toContain(
      'El correo no debe exceder 320 caracteres.'
    );
  });

  it('ubigeoOpcional acepta vacío y seis dígitos', () => {
    expect(messages(ubigeoOpcional, '')).toEqual([]);
    expect(messages(ubigeoOpcional, '150101')).toEqual([]);
    expect(messages(ubigeoOpcional, '1501')).toEqual(['El ubigeo debe tener 6 dígitos.']);
  });

  it('numeroOpcional sin límite acepta vacío y números, y rechaza texto', () => {
    const schema = numeroOpcional('La temperatura');
    expect(messages(schema, '')).toEqual([]);
    expect(messages(schema, '-18.5')).toEqual([]);
    expect(messages(schema, 'abc')).toEqual(['La temperatura debe ser un número.']);
  });

  it('numeroOpcional con límite valida el rango absoluto', () => {
    const schema = numeroOpcional('La latitud', 90);
    expect(messages(schema, '-12.0464')).toEqual([]);
    expect(messages(schema, '91')).toEqual(['La latitud debe estar entre -90 y 90.']);
    expect(messages(schema, 'x')).toEqual(['La latitud debe estar entre -90 y 90.']);
  });
});
