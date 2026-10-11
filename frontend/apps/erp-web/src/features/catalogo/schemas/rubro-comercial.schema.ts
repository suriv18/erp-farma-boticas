import { z } from 'zod';

export const rubroComercialSchema = z.object({
  codigo: z
    .string()
    .min(2, 'El código debe tener al menos 2 caracteres.')
    .max(50, 'El código no debe exceder 50 caracteres.'),
  nombre: z
    .string()
    .min(2, 'El nombre debe tener al menos 2 caracteres.')
    .max(120, 'El nombre no debe exceder 120 caracteres.'),
  descripcion: z.string().max(300, 'La descripción no debe exceder 300 caracteres.').optional(),
  esFarmaceutico: z.boolean(),
  orden: z.number().int().min(0, 'El orden no puede ser negativo.')
});

export type RubroComercialFormValues = z.infer<typeof rubroComercialSchema>;
