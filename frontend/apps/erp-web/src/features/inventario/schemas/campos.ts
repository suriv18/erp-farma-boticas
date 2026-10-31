import { z } from 'zod';

export const MOTIVO_MAX = 1000;

export const motivoRequerido = z
  .string()
  .trim()
  .min(1, 'El motivo es obligatorio.')
  .max(MOTIVO_MAX, `El motivo no debe exceder ${MOTIVO_MAX} caracteres.`);
