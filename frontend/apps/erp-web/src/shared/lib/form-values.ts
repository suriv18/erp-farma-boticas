export function emptyToUndefined(value: string): string | undefined {
  const trimmed = value.trim();
  return trimmed === '' ? undefined : trimmed;
}

export function orEmpty(value: string | null): string {
  return value ?? '';
}
