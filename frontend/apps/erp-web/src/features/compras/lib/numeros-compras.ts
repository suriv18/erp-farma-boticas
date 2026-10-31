const PATRON_CANTIDAD = /^\d{1,9}(\.\d{1,4})?$/;
const PATRON_PRECIO = /^\d{1,10}(\.\d{1,6})?$/;
const PATRON_MONTO = /^\d{1,10}(\.\d{1,2})?$/;
const PATRON_TOLERANCIA = /^\d{1,3}(\.\d{1,4})?$/;

export const esCantidadNoNegativa = (texto: string): boolean => PATRON_CANTIDAD.test(texto);

export const esCantidad = (texto: string): boolean =>
  esCantidadNoNegativa(texto) && Number(texto) > 0;

export const esPrecio = (texto: string): boolean => PATRON_PRECIO.test(texto);

export const esMonto = (texto: string): boolean => PATRON_MONTO.test(texto);

export const esTolerancia = (texto: string): boolean =>
  PATRON_TOLERANCIA.test(texto) && Number(texto) <= 100;
