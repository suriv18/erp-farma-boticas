export function valueOrDash(value: string | null): string {
  return value === null || value === '' ? '—' : value;
}

export function yesNo(value: boolean): string {
  return value ? 'Sí' : 'No';
}

const MONEDA = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' });

export function formatoMoneda(valor: number): string {
  return MONEDA.format(valor);
}

export function formatoFechaHora(instante: string | null): string {
  return instante === null ? '—' : new Date(instante).toLocaleString('es-PE');
}
