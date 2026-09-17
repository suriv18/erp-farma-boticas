import { z } from 'zod';

export const usuarioSchema = z.object({
  firstNames: z.string().max(150, 'Los nombres no deben exceder 150 caracteres.').optional(),
  lastNames: z.string().max(180, 'Los apellidos no deben exceder 180 caracteres.').optional(),
  username: z.string().max(150, 'El usuario no debe exceder 150 caracteres.').optional(),
  email: z.email('Ingresa un correo electrónico válido.').optional().or(z.literal('')),
  phone: z.string().max(40, 'El teléfono no debe exceder 40 caracteres.').optional(),
  displayName: z.string().max(250, 'El nombre visible no debe exceder 250 caracteres.').optional(),
  mfaRequired: z.boolean()
});

export type UsuarioFormValues = z.infer<typeof usuarioSchema>;
