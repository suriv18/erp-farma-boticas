export const UMBRAL_VENCIMIENTO_PROXIMO_DIAS = 90;

const MS_POR_DIA = 86_400_000;

export type NivelVencimiento = 'vencido' | 'proximo' | 'vigente';

export function diasParaVencer(fechaVencimiento: string, hoy: Date): number {
  const inicioHoy = Date.UTC(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
  return Math.round((Date.parse(fechaVencimiento) - inicioHoy) / MS_POR_DIA);
}

export function nivelVencimiento(fechaVencimiento: string, hoy: Date): NivelVencimiento {
  const dias = diasParaVencer(fechaVencimiento, hoy);
  if (dias < 0) return 'vencido';
  return dias <= UMBRAL_VENCIMIENTO_PROXIMO_DIAS ? 'proximo' : 'vigente';
}
