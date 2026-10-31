import { z } from 'zod';

export function textoOpcional(max: number, etiqueta: string) {
  return z.string().max(max, `${etiqueta} no debe exceder ${max} caracteres.`);
}

export const correoOpcional = z
  .string()
  .max(320, 'El correo no debe exceder 320 caracteres.')
  .refine((value) => value === '' || z.email().safeParse(value).success, 'El correo no es válido.');

export const ubigeoOpcional = z.string().regex(/^(\d{6})?$/, 'El ubigeo debe tener 6 dígitos.');

const TELEFONO_CARACTERES = /^[\d\s+\-()]*$/;

function tieneCantidadDigitosValida(valor: string) {
  const digitos = valor.replace(/\D/g, '').length;
  return digitos >= 6 && digitos <= 15;
}

export const telefonoOpcional = z
  .string()
  .max(40, 'El teléfono no debe exceder 40 caracteres.')
  .refine(
    (valor) =>
      valor === '' || (TELEFONO_CARACTERES.test(valor) && tieneCantidadDigitosValida(valor)),
    'El teléfono debe tener entre 6 y 15 dígitos y solo admite números, espacios, +, - y paréntesis.'
  );
