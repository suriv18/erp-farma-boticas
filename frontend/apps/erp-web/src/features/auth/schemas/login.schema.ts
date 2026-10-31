import { z } from 'zod';

const esCorreoValido = (valor: string) =>
  !valor.includes('@') || z.email().safeParse(valor).success;

export const loginSchema = z.object({
  login: z
    .string()
    .trim()
    .min(1, 'Ingresa tu usuario o correo electrónico.')
    .refine(esCorreoValido, 'Ingresa un correo electrónico válido.'),
  password: z.string().min(8, 'La contraseña debe tener al menos 8 caracteres.'),
  remember: z.boolean()
});

export type LoginCredentials = z.infer<typeof loginSchema>;
