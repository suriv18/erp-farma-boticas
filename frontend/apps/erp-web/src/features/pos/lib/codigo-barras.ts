export const esCodigoBarras = (texto: string): boolean => /^\d{8,14}$/.test(texto);
