import { z } from 'zod';
import { fechaOpcional, textoMax } from './campos';

export const codigoBarraSchema = z
  .object({
    codigoBarra: z
      .string()
      .trim()
      .min(1, 'El código de barras es obligatorio.')
      .max(80, 'El código de barras no debe exceder 80 caracteres.'),
    tipoCodigo: textoMax('El tipo de código', 30),
    vigenteDesde: fechaOpcional,
    vigenteHasta: fechaOpcional
  })
  .superRefine((valores, contexto) => {
    if (
      valores.vigenteDesde &&
      valores.vigenteHasta &&
      valores.vigenteHasta < valores.vigenteDesde
    ) {
      contexto.addIssue({
        code: 'custom',
        path: ['vigenteHasta'],
        message: 'La vigencia hasta no puede ser anterior a la vigencia desde.'
      });
    }
  });

export type CodigoBarraFormValues = z.infer<typeof codigoBarraSchema>;
