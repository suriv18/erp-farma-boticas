export function emptyToUndefined(value: string): string | undefined {
  const trimmed = value.trim();
  return trimmed === '' ? undefined : trimmed;
}

export function orEmpty(value: string | null): string {
  return value ?? '';
}

export function numeroOpcional(value: string): number | undefined {
  const limpio = emptyToUndefined(value);
  return limpio === undefined ? undefined : Number(limpio);
}
