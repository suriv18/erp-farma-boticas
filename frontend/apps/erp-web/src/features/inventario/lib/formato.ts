export const codigoCorto = (id: string): string => id.slice(0, 8);

export function formatearInstante(instante: string | null): string {
  return instante === null ? '—' : new Date(instante).toLocaleString('es-PE');
}
