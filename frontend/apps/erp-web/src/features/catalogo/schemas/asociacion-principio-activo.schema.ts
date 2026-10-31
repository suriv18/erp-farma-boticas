import { z } from 'zod';
import { decimal, textoMax } from './campos';

const ORDEN_MAXIMO = 32767;

export const asociacionPrincipioActivoSchema = z.object({
  principioActivoId: z.string().min(1, 'Selecciona un principio activo.'),
  concentracionTexto: textoMax('La concentración', 200),
  cantidad: decimal({
    etiqueta: 'La cantidad',
    enteros: 12,
    decimales: 6,
    requerido: false,
    permiteCero: false
  }),
  unidadMedidaCodigo: z.string(),
  esPrincipal: z.boolean(),
  orden: z
    .string()
    .refine(
      (valor) => /^\d{1,5}$/.test(valor) && Number(valor) <= ORDEN_MAXIMO,
      `El orden debe ser un entero entre 0 y ${ORDEN_MAXIMO}.`
    )
});

export type AsociacionPrincipioActivoFormValues = z.infer<typeof asociacionPrincipioActivoSchema>;
