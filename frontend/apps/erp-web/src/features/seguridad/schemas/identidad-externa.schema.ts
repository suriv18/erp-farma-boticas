import { z } from 'zod';

const KNOWN_PROVIDERS = ['GOOGLE', 'MICROSOFT', 'SAML'] as const;

export const identidadExternaSchema = z
  .object({
    providerOption: z.enum([...KNOWN_PROVIDERS, 'OTRO'], { message: 'Selecciona un proveedor.' }),
    providerCustom: z.string().max(100, 'El proveedor no debe exceder 100 caracteres.').optional(),
    subject: z
      .string()
      .min(1, 'El identificador del sujeto es obligatorio.')
      .max(300, 'El sujeto no debe exceder 300 caracteres.'),
    issuer: z.string().max(500, 'El emisor no debe exceder 500 caracteres.').optional(),
    emailClaim: z.email('Ingresa un correo electrónico válido.').max(254).optional().or(z.literal(''))
  })
  .refine((values) => values.providerOption !== 'OTRO' || Boolean(values.providerCustom?.trim()), {
    message: 'Indica el nombre del proveedor.',
    path: ['providerCustom']
  });

export type IdentidadExternaFormValues = z.infer<typeof identidadExternaSchema>;

export { KNOWN_PROVIDERS };
