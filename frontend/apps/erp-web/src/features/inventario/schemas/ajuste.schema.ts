import { z } from 'zod';
import { TIPOS_AJUSTE } from '../api/inventario.types';
import { motivoRequerido } from './campos';

const NUMERO_LOTE_MAX = 120;

export const ajusteSchema = z
  .object({
    almacenId: z.string().min(1, 'Selecciona un almacén.'),
    skuId: z.string().min(1, 'Selecciona un SKU.'),
    tipo: z.enum(TIPOS_AJUSTE, { error: 'Selecciona el tipo de ajuste.' }),
    loteId: z.string(),
    numeroLote: z
      .string()
      .max(NUMERO_LOTE_MAX, `El número de lote no debe exceder ${NUMERO_LOTE_MAX} caracteres.`),
    fechaVencimiento: z.string(),
    cantidad: z
      .string()
      .refine(
        (valor) => Number.isFinite(Number(valor)) && Number(valor) > 0,
        'La cantidad debe ser un número mayor que cero.'
      ),
    motivo: motivoRequerido
  })
  .refine((valores) => valores.tipo !== 'AJUSTE_SALIDA' || valores.loteId !== '', {
    path: ['loteId'],
    error: 'Una salida requiere un lote existente.'
  })
  .refine(
    (valores) =>
      valores.tipo !== 'AJUSTE_INGRESO' ||
      valores.loteId !== '' ||
      valores.numeroLote.trim() !== '',
    { path: ['numeroLote'], error: 'Indica el número del lote.' }
  )
  .refine(
    (valores) =>
      valores.tipo !== 'AJUSTE_INGRESO' || valores.loteId !== '' || valores.fechaVencimiento !== '',
    { path: ['fechaVencimiento'], error: 'Indica la fecha de vencimiento.' }
  );

export type AjusteFormValues = z.infer<typeof ajusteSchema>;
