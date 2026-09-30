import { z } from 'zod';
import { PERFILES_OPERACION, TIPOS_ESTABLECIMIENTO } from '../api/establecimientos.types';
import {
  codigoRequerido,
  correoOpcional,
  nombreRequerido,
  numeroOpcional,
  textoOpcional,
  ubigeoOpcional,
  zonaHorariaRequerida
} from './campos';

export const establecimientoSchema = z.object({
  codigo: codigoRequerido,
  nombre: nombreRequerido(250),
  tipoEstablecimiento: z.enum(TIPOS_ESTABLECIMIENTO, {
    error: 'Selecciona un tipo de establecimiento.'
  }),
  categoriaRegulatoriaCodigo: textoOpcional(50, 'La categoría regulatoria'),
  codigoAnexoSunat: z.string().regex(/^\d{4}$/, 'El anexo SUNAT debe tener 4 dígitos.'),
  codigoDigemid: textoOpcional(10, 'El código DIGEMID'),
  direccion: textoOpcional(500, 'La dirección'),
  ubigeo: ubigeoOpcional,
  referencia: textoOpcional(300, 'La referencia'),
  latitud: numeroOpcional('La latitud', 90),
  longitud: numeroOpcional('La longitud', 180),
  telefono: textoOpcional(40, 'El teléfono'),
  email: correoOpcional,
  esPrincipal: z.boolean(),
  permiteVentaOnline: z.boolean(),
  permiteDelivery: z.boolean(),
  perfilOperacion: z.enum(PERFILES_OPERACION, { error: 'Selecciona un perfil de operación.' }),
  zonaHoraria: zonaHorariaRequerida
});

export type EstablecimientoFormValues = z.infer<typeof establecimientoSchema>;
