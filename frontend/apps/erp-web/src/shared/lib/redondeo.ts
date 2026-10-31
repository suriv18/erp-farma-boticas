const PRECISION_SIGNIFICATIVA = 15;

const aEscala = (valor: number, exponente: number): number => Number(`${valor}e${exponente}`);

export function redondear(valor: number, decimales: number): number {
  const limpio = Number(Math.abs(valor).toPrecision(PRECISION_SIGNIFICATIVA));
  const redondeado = aEscala(Math.round(aEscala(limpio, decimales)), -decimales);
  return Math.sign(valor) * redondeado || 0;
}
