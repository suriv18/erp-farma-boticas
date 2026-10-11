import type { Posicion, RegistrarMovimientoPayload, TipoAjuste } from '../api/inventario.types';
import type { AjusteFormValues } from '../schemas/ajuste.schema';

export const ETIQUETAS_TIPO_AJUSTE: Record<TipoAjuste, string> = {
  AJUSTE_INGRESO: 'Ingreso',
  AJUSTE_SALIDA: 'Salida'
};

export const AJUSTE_FORM_VACIO: AjusteFormValues = {
  almacenId: '',
  skuId: '',
  tipo: 'AJUSTE_INGRESO',
  loteId: '',
  numeroLote: '',
  fechaVencimiento: '',
  cantidad: '',
  motivo: ''
};

export const ajusteDesdePosicion = (posicion: Posicion): AjusteFormValues => ({
  ...AJUSTE_FORM_VACIO,
  almacenId: posicion.almacenId,
  skuId: posicion.skuId,
  loteId: posicion.loteId
});

export function toRegistrarMovimientoPayload(values: AjusteFormValues): RegistrarMovimientoPayload {
  return {
    almacenId: values.almacenId,
    skuId: values.skuId,
    tipo: values.tipo,
    cantidad: Number(values.cantidad),
    motivo: values.motivo,
    ...(values.loteId
      ? { loteId: values.loteId }
      : { numeroLote: values.numeroLote.trim(), fechaVencimiento: values.fechaVencimiento })
  };
}
