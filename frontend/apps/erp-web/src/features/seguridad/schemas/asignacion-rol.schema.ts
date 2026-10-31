import { z } from 'zod';

const SCOPE_TYPES = ['GLOBAL', 'EMPRESA', 'ESTABLECIMIENTO', 'ALMACEN', 'TERMINAL'] as const;

export const asignacionRolSchema = z
  .object({
    roleId: z.string().min(1, 'Selecciona un rol.'),
    scopeType: z.enum(SCOPE_TYPES, { message: 'Selecciona un tipo de ámbito válido.' }),
    companyId: z.string().optional(),
    establishmentId: z.string().optional(),
    warehouseId: z.string().optional(),
    terminalId: z.string().optional(),
    validFrom: z.string().optional(),
    validUntil: z.string().optional()
  })
  .refine((values) => values.scopeType !== 'EMPRESA' || Boolean(values.companyId), {
    message: 'Selecciona una empresa.',
    path: ['companyId']
  })
  .refine((values) => values.scopeType !== 'ESTABLECIMIENTO' || Boolean(values.establishmentId), {
    message: 'Selecciona un establecimiento.',
    path: ['establishmentId']
  })
  .refine((values) => values.scopeType !== 'ALMACEN' || Boolean(values.warehouseId), {
    message: 'Selecciona un almacén.',
    path: ['warehouseId']
  })
  .refine((values) => values.scopeType !== 'TERMINAL' || Boolean(values.terminalId), {
    message: 'Selecciona un terminal.',
    path: ['terminalId']
  });

export type AsignacionRolFormValues = z.infer<typeof asignacionRolSchema>;

export { SCOPE_TYPES };
