import { z } from 'zod';
import {
  correoOpcional,
  telefonoOpcional,
  textoOpcional,
  ubigeoOpcional
} from '../../../shared/schemas/campos';

const TIPO_RUC = '6';
const RUC_PROVEEDOR = /^(10|20)\d{9}$/;

const correoHasta254 = correoOpcional.refine(
  (value) => value.length <= 254,
  'El correo no debe exceder 254 caracteres.'
);

export const proveedorSchema = z
  .object({
    tipoDocumento: z.string().max(2, 'El tipo de documento admite hasta 2 caracteres.'),
    numeroDocumento: z
      .string()
      .min(1, 'El número de documento es obligatorio.')
      .max(15, 'El número de documento admite hasta 15 caracteres.'),
    razonSocial: z
      .string()
      .trim()
      .min(1, 'La razón social es obligatoria.')
      .max(300, 'La razón social no debe exceder 300 caracteres.'),
    nombreComercial: textoOpcional(300, 'El nombre comercial'),
    direccion: textoOpcional(500, 'La dirección'),
    ubigeo: ubigeoOpcional,
    telefono: telefonoOpcional,
    email: correoHasta254,
    contactoNombre: textoOpcional(180, 'El contacto'),
    contactoTelefono: telefonoOpcional,
    contactoEmail: correoHasta254,
    condicionPagoDefault: textoOpcional(80, 'La condición de pago'),
    diasCreditoDefault: z
      .string()
      .regex(/^\d{1,4}$/, 'Los días de crédito deben ser un entero mayor o igual a 0.'),
    monedaDefault: z
      .string()
      .regex(/^[A-Z]{3}$/, 'La moneda debe ser un código de 3 letras mayúsculas.'),
    calificacion: textoOpcional(30, 'La calificación'),
    esLaboratorio: z.boolean(),
    esImportador: z.boolean(),
    esDistribuidor: z.boolean()
  })
  .superRefine((valores, contexto) => {
    const esRuc = valores.tipoDocumento === '' || valores.tipoDocumento === TIPO_RUC;
    if (esRuc && !RUC_PROVEEDOR.test(valores.numeroDocumento)) {
      contexto.addIssue({
        code: 'custom',
        path: ['numeroDocumento'],
        message: 'El RUC debe tener 11 dígitos y empezar con 10 o 20.'
      });
    }
  });

export type ProveedorFormValues = z.infer<typeof proveedorSchema>;
