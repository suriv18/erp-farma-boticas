import type { AsociarPrincipioActivoPayload } from '../api/productos-regulados.types';
import type { AsociacionPrincipioActivoFormValues } from '../schemas/asociacion-principio-activo.schema';
import { numeroOpcional, textoOpcional } from './form-values';

export const ASOCIACION_PRINCIPIO_ACTIVO_VACIA: AsociacionPrincipioActivoFormValues = {
  principioActivoId: '',
  concentracionTexto: '',
  cantidad: '',
  unidadMedidaCodigo: '',
  esPrincipal: true,
  orden: '1'
};

export function toAsociarPayload(
  values: AsociacionPrincipioActivoFormValues
): AsociarPrincipioActivoPayload {
  return {
    principioActivoId: values.principioActivoId,
    concentracionTexto: textoOpcional(values.concentracionTexto),
    cantidad: numeroOpcional(values.cantidad),
    unidadMedidaCodigo: textoOpcional(values.unidadMedidaCodigo),
    esPrincipal: values.esPrincipal,
    orden: Number(values.orden)
  };
}
