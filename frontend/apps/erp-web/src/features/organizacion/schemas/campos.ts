import { z } from 'zod';

export {
  correoOpcional,
  telefonoOpcional,
  textoOpcional,
  ubigeoOpcional
} from '../../../shared/schemas/campos';

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

export const sitioWebOpcional = z
  .string()
  .max(300, 'El sitio web no debe exceder 300 caracteres.')
  .refine(
    (valor) => valor === '' || (z.url().safeParse(valor).success && /^https?:\/\//i.test(valor)),
    'El sitio web debe ser una URL que empiece con http:// o https://.'
  );
