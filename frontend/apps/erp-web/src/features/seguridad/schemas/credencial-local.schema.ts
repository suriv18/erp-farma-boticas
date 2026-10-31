import { z } from 'zod';

const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).+$/;

export const credencialLocalSchema = z
  .object({
    password: z
      .string()
      .min(12, 'La contraseña debe tener al menos 12 caracteres.')
      .max(128, 'La contraseña no debe exceder 128 caracteres.')
      .regex(PASSWORD_REGEX, 'Debe incluir mayúscula, minúscula, número y símbolo.'),
    confirmPassword: z.string(),
    requireChange: z.boolean()
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: 'Las contraseñas no coinciden.',
    path: ['confirmPassword']
  });

export type CredencialLocalFormValues = z.infer<typeof credencialLocalSchema>;
