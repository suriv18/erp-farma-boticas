import type { MiPuestoEstablecimiento } from '../api/mi-puesto.types';

type Seleccion = { establecimientoId: string; terminalId: string };

export function resolverPuesto(
  establecimientos: MiPuestoEstablecimiento[],
  actual: Seleccion
): Seleccion | null {
  const unico = actual.establecimientoId === '' && establecimientos.length === 1;
  const elegido = unico
    ? establecimientos[0]
    : establecimientos.find(({ id }) => id === actual.establecimientoId);
  if (!elegido) {
    return actual.establecimientoId === '' ? null : { establecimientoId: '', terminalId: '' };
  }
  const operables = elegido.terminales.filter(({ operable }) => operable);
  const [primera, ...resto] = operables;
  const vigente = operables.some(({ id }) => id === actual.terminalId);
  const terminalId = vigente ? actual.terminalId : primera && resto.length === 0 ? primera.id : '';
  return elegido.id === actual.establecimientoId && terminalId === actual.terminalId
    ? null
    : { establecimientoId: elegido.id, terminalId };
}
