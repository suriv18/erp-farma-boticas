import { z } from 'zod';

export const categoriaSchema = z.object({
  categoriaPadreId: z.string().optional(),
  codigo: z
    .string()
    .min(2, 'El código debe tener al menos 2 caracteres.')
    .max(50, 'El código no debe exceder 50 caracteres.'),
  nombre: z
    .string()
    .min(2, 'El nombre debe tener al menos 2 caracteres.')
    .max(180, 'El nombre no debe exceder 180 caracteres.'),
  descripcion: z.string().max(500, 'La descripción no debe exceder 500 caracteres.').optional(),
  nivel: z.number().int().min(1, 'El nivel debe ser mayor o igual a 1.'),
  orden: z.number().int().min(0, 'El orden debe ser mayor o igual a 0.')
});

export type CategoriaFormValues = z.infer<typeof categoriaSchema>;
