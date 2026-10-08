import { z } from 'zod';

export function textoMax(etiqueta: string, max: number) {
  return z.string().max(max, `${etiqueta} no debe exceder ${max} caracteres.`);
}

export function textoRango(etiqueta: string, min: number, max: number) {
  return z
    .string()
    .trim()
    .min(min, `${etiqueta} debe tener al menos ${min} caracteres.`)
    .max(max, `${etiqueta} no debe exceder ${max} caracteres.`);
}

export const fechaOpcional = z
  .string()
  .regex(/^(\d{4}-\d{2}-\d{2})?$/, 'Ingresa una fecha válida.');

export type OpcionesDecimal = {
  etiqueta: string;
  enteros: number;
  decimales: number;
  requerido: boolean;
  permiteCero: boolean;
};

export function decimal({ etiqueta, enteros, decimales, requerido, permiteCero }: OpcionesDecimal) {
  const patron = new RegExp(`^\\d{1,${enteros}}(\\.\\d{1,${decimales}})?$`);
  const minimo = permiteCero ? 0 : Number.MIN_VALUE;
  const condicion = permiteCero ? 'mayor o igual a cero' : 'mayor que cero';
  return z
    .string()
    .refine(
      (valor) => (valor === '' ? !requerido : patron.test(valor) && Number(valor) >= minimo),
      `${etiqueta} debe ser un número ${condicion} con hasta ${decimales} decimales.`
    );
}
