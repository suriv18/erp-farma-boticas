import { z } from 'zod';

export const marcaSchema = z.object({
  codigo: z
    .string()
    .min(2, 'El código debe tener al menos 2 caracteres.')
    .max(50, 'El código no debe exceder 50 caracteres.'),
  nombre: z
    .string()
    .min(2, 'El nombre debe tener al menos 2 caracteres.')
    .max(180, 'El nombre no debe exceder 180 caracteres.'),
  descripcion: z.string().max(500, 'La descripción no debe exceder 500 caracteres.').optional()
});

export type MarcaFormValues = z.infer<typeof marcaSchema>;
