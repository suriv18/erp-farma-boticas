import { z } from 'zod';

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Ingresa tu contraseña actual.'),
    newPassword: z.string().min(8, 'La nueva contraseña debe tener al menos 8 caracteres.'),
    confirmPassword: z.string()
  })
  .refine(({ newPassword, confirmPassword }) => newPassword === confirmPassword, {
    message: 'Las contraseñas no coinciden.',
    path: ['confirmPassword']
  })
  .refine(({ currentPassword, newPassword }) => currentPassword !== newPassword, {
    message: 'La nueva contraseña debe ser distinta de la actual.',
    path: ['newPassword']
  });

export type ChangePasswordValues = z.infer<typeof changePasswordSchema>;
