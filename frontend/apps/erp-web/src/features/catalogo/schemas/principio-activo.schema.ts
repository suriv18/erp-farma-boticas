import { z } from 'zod';
import { textoMax, textoRango } from './campos';

export const principioActivoSchema = z.object({
  codigoFuente: textoMax('El código fuente', 80),
  denominacion: textoRango('La denominación', 2, 300),
  nombreNormalizado: textoMax('El nombre normalizado', 300),
  fuente: textoMax('La fuente', 300)
});

export type PrincipioActivoFormValues = z.infer<typeof principioActivoSchema>;
