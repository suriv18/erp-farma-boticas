import { redondear } from '../../../shared/lib/redondeo';

export const TASA_IGV = 0.18;

export const impuestoSugerido = (base: number, afectoIgv: boolean): number =>
  afectoIgv ? redondear(base * TASA_IGV, 2) : 0;
