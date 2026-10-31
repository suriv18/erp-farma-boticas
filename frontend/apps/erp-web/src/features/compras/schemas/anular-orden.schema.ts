import { z } from 'zod';

export const anularOrdenSchema = z.object({
  motivo: z
    .string()
    .trim()
    .min(1, 'El motivo es obligatorio.')
    .max(300, 'El motivo no debe exceder 300 caracteres.')
});

export type AnularOrdenFormValues = z.infer<typeof anularOrdenSchema>;
