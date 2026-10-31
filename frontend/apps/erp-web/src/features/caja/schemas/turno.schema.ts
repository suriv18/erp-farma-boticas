import { z } from 'zod';

const OBSERVACION_MAX = 1000;
export const PATRON_MONTO = /^\d{1,9}(\.\d{1,2})?$/;

const monto = (etiqueta: string) =>
  z
    .string()
    .refine(
      (valor) => PATRON_MONTO.test(valor),
      `${etiqueta} debe ser un monto mayor o igual a cero con hasta 2 decimales.`
    );

export const abrirTurnoSchema = z.object({ fondoInicial: monto('El fondo inicial') });

export const cerrarTurnoSchema = z.object({
  totalDeclarado: monto('El total declarado'),
  observacion: z
    .string()
    .max(OBSERVACION_MAX, `La observación no debe exceder ${OBSERVACION_MAX} caracteres.`)
});

export type AbrirTurnoFormValues = z.infer<typeof abrirTurnoSchema>;
export type CerrarTurnoFormValues = z.infer<typeof cerrarTurnoSchema>;
