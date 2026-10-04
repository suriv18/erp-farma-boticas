import { usePreferenciaLocal } from '../../../shared/lib/use-preferencia-local';

export type PuestoTrabajo = {
  establecimientoId: string;
  terminalId: string;
  almacenId: string;
};

export const PUESTO_VACIO: PuestoTrabajo = { establecimientoId: '', terminalId: '', almacenId: '' };

export function usePuestoTrabajo() {
  const [puesto, setPuesto] = usePreferenciaLocal<PuestoTrabajo>(
    'erp.puesto-trabajo',
    PUESTO_VACIO
  );
  return { puesto, setPuesto };
}
