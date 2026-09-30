import { z } from 'zod';
import { RUC_FORMATO, tieneDigitoVerificadorValido } from '../lib/ruc';
import {
  correoOpcional,
  sitioWebOpcional,
  telefonoOpcional,
  textoOpcional,
  ubigeoOpcional,
  zonaHorariaRequerida
} from './campos';

const MENSAJE_FORMATO_RUC = 'El RUC debe tener 11 dígitos e iniciar con 10 o 20.';

const rucConFormato = z.string().regex(RUC_FORMATO, MENSAJE_FORMATO_RUC);

const camposEmpresa = {
  razonSocial: z
    .string()
    .min(2, 'La razón social debe tener al menos 2 caracteres.')
    .max(300, 'La razón social no debe exceder 300 caracteres.'),
  nombreComercial: textoOpcional(300, 'El nombre comercial'),
  direccionFiscal: textoOpcional(500, 'La dirección fiscal'),
  ubigeoFiscal: ubigeoOpcional,
  telefono: telefonoOpcional,
  email: correoOpcional,
  sitioWeb: sitioWebOpcional,
  monedaFuncional: z.string().length(3, 'La moneda debe tener 3 caracteres (ISO 4217).'),
  zonaHoraria: zonaHorariaRequerida,
  permiteVentaOnline: z.boolean()
};

export const empresaSchema = z.object({
  ruc: rucConFormato.refine(
    (ruc) => !RUC_FORMATO.test(ruc) || tieneDigitoVerificadorValido(ruc),
    'El RUC no es válido: el dígito verificador no coincide.'
  ),
  ...camposEmpresa
});

export const empresaEdicionSchema = z.object({ ruc: rucConFormato, ...camposEmpresa });

export type EmpresaFormValues = z.infer<typeof empresaSchema>;
