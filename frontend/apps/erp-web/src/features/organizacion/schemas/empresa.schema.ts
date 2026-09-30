import { z } from 'zod';
import { correoOpcional, textoOpcional, ubigeoOpcional, zonaHorariaRequerida } from './campos';

export const empresaSchema = z.object({
  ruc: z.string().regex(/^(10|20)\d{9}$/, 'El RUC debe tener 11 dígitos e iniciar con 10 o 20.'),
  razonSocial: z
    .string()
    .min(2, 'La razón social debe tener al menos 2 caracteres.')
    .max(300, 'La razón social no debe exceder 300 caracteres.'),
  nombreComercial: textoOpcional(300, 'El nombre comercial'),
  direccionFiscal: textoOpcional(500, 'La dirección fiscal'),
  ubigeoFiscal: ubigeoOpcional,
  telefono: textoOpcional(40, 'El teléfono'),
  email: correoOpcional,
  sitioWeb: textoOpcional(300, 'El sitio web'),
  monedaFuncional: z.string().length(3, 'La moneda debe tener 3 caracteres (ISO 4217).'),
  zonaHoraria: zonaHorariaRequerida,
  permiteVentaOnline: z.boolean()
});

export type EmpresaFormValues = z.infer<typeof empresaSchema>;
