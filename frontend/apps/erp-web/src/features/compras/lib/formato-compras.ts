export function formatoImporte(valor: number, moneda: string): string {
  try {
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: moneda }).format(valor);
  } catch {
    return `${moneda} ${valor.toFixed(2)}`;
  }
}

export function formatoFecha(fecha: string | null): string {
  if (fecha === null) return '—';
  const [anio = '0', mes = '1', dia = '1'] = fecha.split('-');
  return new Date(Number(anio), Number(mes) - 1, Number(dia)).toLocaleDateString('es-PE');
}
