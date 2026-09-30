import { z } from 'zod';
import { ESTADOS_TERMINAL } from '../api/terminales.types';
import { codigoRequerido, nombreRequerido, textoOpcional } from './campos';

export const terminalSchema = z.object({
  codigo: codigoRequerido,
  nombre: nombreRequerido(120),
  serieBoletaDefecto: z
    .string()
    .regex(/^(B[A-Z0-9]{3})?$/, 'La serie de boleta debe iniciar con B y tener 4 caracteres.'),
  serieFacturaDefecto: z
    .string()
    .regex(/^(F[A-Z0-9]{3})?$/, 'La serie de factura debe iniciar con F y tener 4 caracteres.'),
  numeroSerieEquipo: textoOpcional(120, 'El número de serie'),
  hostname: textoOpcional(150, 'El hostname'),
  ipEquipo: z.union([z.literal(''), z.ipv4(), z.ipv6()], {
    error: 'La dirección IP no es válida.'
  }),
  impresoraCodigo: textoOpcional(100, 'El código de impresora'),
  storeEdgeHabilitado: z.boolean(),
  estado: z.enum(ESTADOS_TERMINAL, { error: 'Selecciona un estado.' })
});

export type TerminalFormValues = z.infer<typeof terminalSchema>;
