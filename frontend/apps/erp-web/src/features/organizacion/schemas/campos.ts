import { z } from 'zod';

export function textoOpcional(max: number, etiqueta: string) {
  return z.string().max(max, `${etiqueta} no debe exceder ${max} caracteres.`);
}

export function nombreRequerido(max: number) {
  return z
    .string()
    .min(2, 'El nombre debe tener al menos 2 caracteres.')
    .max(max, `El nombre no debe exceder ${max} caracteres.`);
}

export const codigoRequerido = z
  .string()
  .min(1, 'El código es obligatorio.')
  .max(40, 'El código no debe exceder 40 caracteres.');

export const zonaHorariaRequerida = z
  .string()
  .min(1, 'La zona horaria es obligatoria.')
  .max(80, 'La zona horaria no debe exceder 80 caracteres.');

export const correoOpcional = z
  .string()
  .max(320, 'El correo no debe exceder 320 caracteres.')
  .refine((value) => value === '' || z.email().safeParse(value).success, 'El correo no es válido.');

export const ubigeoOpcional = z.string().regex(/^(\d{6})?$/, 'El ubigeo debe tener 6 dígitos.');

export function numeroOpcional(etiqueta: string, limite?: number) {
  return z
    .string()
    .refine(
      (value) =>
        value === '' ||
        (Number.isFinite(Number(value)) &&
          (limite === undefined || Math.abs(Number(value)) <= limite)),
      limite === undefined
        ? `${etiqueta} debe ser un número.`
        : `${etiqueta} debe estar entre -${limite} y ${limite}.`
    );
}
