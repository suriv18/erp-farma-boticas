export { emptyToUndefined, orEmpty } from '../../../shared/lib/form-values';

export function toNumberOrUndefined(value: string): number | undefined {
  const trimmed = value.trim();
  return trimmed === '' ? undefined : Number(trimmed);
}

export function numberOrEmpty(value: number | null): string {
  return value === null ? '' : String(value);
}

export function aMayusculasSinEspacios(value: string): string {
  return value.replace(/\s/g, '').toUpperCase();
}
