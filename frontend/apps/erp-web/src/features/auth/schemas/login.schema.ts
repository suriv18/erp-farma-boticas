import { z } from 'zod';

export const loginSchema = z.object({
  tenantId: z.uuid('Ingresa un identificador de organización válido.'),
  email: z.email('Ingresa un correo electrónico válido.'),
  password: z.string().min(8, 'La contraseña debe tener al menos 8 caracteres.'),
  remember: z.boolean()
});

export type LoginCredentials = z.infer<typeof loginSchema>;
