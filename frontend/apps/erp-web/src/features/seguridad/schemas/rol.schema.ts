import { z } from 'zod';

export const rolSchema = z.object({
  code: z
    .string()
    .min(3, 'El código debe tener al menos 3 caracteres.')
    .max(80, 'El código no debe exceder 80 caracteres.'),
  name: z
    .string()
    .min(2, 'El nombre debe tener al menos 2 caracteres.')
    .max(150, 'El nombre no debe exceder 150 caracteres.'),
  description: z.string().max(500, 'La descripción no debe exceder 500 caracteres.').optional(),
  roleType: z.enum(['GLOBAL', 'EMPRESA', 'ESTABLECIMIENTO', 'ALMACEN', 'TERMINAL'], {
    message: 'Selecciona un tipo de rol válido.'
  }),
  systemRole: z.boolean()
});

export type RolFormValues = z.infer<typeof rolSchema>;
