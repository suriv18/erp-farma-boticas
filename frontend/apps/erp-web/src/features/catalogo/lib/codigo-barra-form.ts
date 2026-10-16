import type { CodigoBarraFormValues } from '../schemas/codigo-barra.schema';

export const CODIGO_BARRA_VACIO: CodigoBarraFormValues = {
  codigoBarra: '',
  tipoCodigo: 'EAN13',
  vigenteDesde: '',
  vigenteHasta: ''
};
