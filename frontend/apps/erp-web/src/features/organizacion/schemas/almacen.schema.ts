import { z } from 'zod';
import { TIPOS_ALMACEN } from '../api/almacenes.types';
import { codigoRequerido, nombreRequerido, numeroOpcional } from './campos';

const REQUIERE_AMBAS_TEMPERATURAS =
  'Indica la temperatura mínima y máxima cuando el almacén controla temperatura.';

export const almacenSchema = z
  .object({
    codigo: codigoRequerido,
    nombre: nombreRequerido(150),
    tipo: z.enum(TIPOS_ALMACEN, { error: 'Selecciona un tipo de almacén.' }),
    permiteLotes: z.boolean(),
    permiteVencimiento: z.boolean(),
    permiteVenta: z.boolean(),
    permiteDespacho: z.boolean(),
    controlTemperatura: z.boolean(),
    temperaturaMinC: numeroOpcional('La temperatura mínima'),
    temperaturaMaxC: numeroOpcional('La temperatura máxima'),
    activo: z.boolean()
  })
  .refine((valores) => valores.tipo !== 'REFRIGERADO' || valores.controlTemperatura, {
    path: ['controlTemperatura'],
    error: 'Un almacén refrigerado debe controlar temperatura.'
  })
  .refine((valores) => !valores.controlTemperatura || valores.temperaturaMinC !== '', {
    path: ['temperaturaMinC'],
    error: REQUIERE_AMBAS_TEMPERATURAS
  })
  .refine((valores) => !valores.controlTemperatura || valores.temperaturaMaxC !== '', {
    path: ['temperaturaMaxC'],
    error: REQUIERE_AMBAS_TEMPERATURAS
  })
  .refine(
    (valores) =>
      valores.temperaturaMinC === '' ||
      valores.temperaturaMaxC === '' ||
      Number(valores.temperaturaMinC) <= Number(valores.temperaturaMaxC),
    {
      path: ['temperaturaMinC'],
      error: 'La temperatura mínima no puede ser mayor que la máxima.'
    }
  );

export type AlmacenFormValues = z.infer<typeof almacenSchema>;
