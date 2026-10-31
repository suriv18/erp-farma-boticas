export const RUC_FORMATO = /^(10|20)\d{9}$/;

const PESOS = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2] as const;

export function tieneDigitoVerificadorValido(ruc: string): boolean {
  const suma = PESOS.reduce((total, peso, indice) => total + Number(ruc[indice]) * peso, 0);
  return (11 - (suma % 11)) % 10 === Number(ruc[10]);
}
