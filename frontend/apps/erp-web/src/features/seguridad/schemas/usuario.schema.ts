import { z } from 'zod';

export const usuarioSchema = z.object({
  documentType: z
    .string()
    .length(1, 'Selecciona un tipo de documento.'),
  documentNumber: z
    .string()
    .min(1, 'Ingresa el número de documento.')
    .max(30, 'El número de documento no debe exceder 30 caracteres.'),
  firstNames: z.string().max(150, 'Los nombres no deben exceder 150 caracteres.').optional(),
  lastNames: z.string().max(180, 'Los apellidos no deben exceder 180 caracteres.').optional(),
  username: z.string().max(150, 'El usuario no debe exceder 150 caracteres.').optional(),
  email: z.email('Ingresa un correo electrónico válido.').max(254, 'El correo no debe exceder 254 caracteres.'),
  phone: z.string().max(40, 'El teléfono no debe exceder 40 caracteres.').optional(),
  displayName: z.string().max(250, 'El nombre visible no debe exceder 250 caracteres.').optional(),
  mfaRequired: z.boolean()
});

export type UsuarioFormValues = z.infer<typeof usuarioSchema>;
