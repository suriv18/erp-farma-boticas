export function valueOrDash(value: string | null): string {
  return value === null || value === '' ? '—' : value;
}

export function yesNo(value: boolean): string {
  return value ? 'Sí' : 'No';
}
