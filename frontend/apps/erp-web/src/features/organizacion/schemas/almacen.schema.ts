import { z } from 'zod';
import { TIPOS_ALMACEN } from '../api/almacenes.types';
import { codigoRequerido, nombreRequerido, numeroOpcional } from './campos';

export const almacenSchema = z.object({
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
});

export type AlmacenFormValues = z.infer<typeof almacenSchema>;
