import type { ZodType } from 'zod';
import {
  correoOpcional,
  numeroOpcional,
  sitioWebOpcional,
  telefonoOpcional,
  textoOpcional,
  ubigeoOpcional
} from './campos';

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

  it('telefonoOpcional acepta vacío y teléfonos con 6 a 15 dígitos, espacios, +, - y paréntesis', () => {
    expect(messages(telefonoOpcional, '')).toEqual([]);
    expect(messages(telefonoOpcional, '014445566')).toEqual([]);
    expect(messages(telefonoOpcional, '+51 (1) 444-5566')).toEqual([]);
    expect(messages(telefonoOpcional, '123456')).toEqual([]);
    expect(messages(telefonoOpcional, '123456789012345')).toEqual([]);
  });

  it('telefonoOpcional rechaza letras, símbolos y cantidades de dígitos fuera de rango', () => {
    const mensaje =
      'El teléfono debe tener entre 6 y 15 dígitos y solo admite números, espacios, +, - y paréntesis.';
    expect(messages(telefonoOpcional, 'abc')).toEqual([mensaje]);
    expect(messages(telefonoOpcional, '12345')).toEqual([mensaje]);
    expect(messages(telefonoOpcional, '1234567890123456')).toEqual([mensaje]);
    expect(messages(telefonoOpcional, '444-5566 ext.')).toEqual([mensaje]);
    expect(messages(telefonoOpcional, '444#5566')).toEqual([mensaje]);
  });

  it('telefonoOpcional informa solo la longitud cuando excede 40 caracteres válidos', () => {
    expect(messages(telefonoOpcional, `123456${' '.repeat(35)}`)).toEqual([
      'El teléfono no debe exceder 40 caracteres.'
    ]);
  });

  it('sitioWebOpcional acepta vacío y URL http o https', () => {
    expect(messages(sitioWebOpcional, '')).toEqual([]);
    expect(messages(sitioWebOpcional, 'https://boticas.pe')).toEqual([]);
    expect(messages(sitioWebOpcional, 'http://boticas.pe/ayuda')).toEqual([]);
  });

  it('sitioWebOpcional rechaza texto que no es URL http(s) y textos largos', () => {
    const mensaje = 'El sitio web debe ser una URL que empiece con http:// o https://.';
    expect(messages(sitioWebOpcional, 'x')).toEqual([mensaje]);
    expect(messages(sitioWebOpcional, 'ftp://boticas.pe')).toEqual([mensaje]);
    expect(messages(sitioWebOpcional, `https://${'a'.repeat(300)}.pe`)).toContain(
      'El sitio web no debe exceder 300 caracteres.'
    );
  });
});
