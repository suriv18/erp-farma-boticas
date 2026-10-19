export { numeroOpcional } from '../../../shared/lib/form-values';

export function textoOpcional(value: string | undefined): string | undefined {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}

export function textoDeFormulario(value: string | null | undefined): string {
  return value ?? '';
}

export function numeroDeFormulario(value: number | null | undefined): string {
  return value === null || value === undefined ? '' : String(value);
}

export function aOpcionales<T extends Record<string, string>>(
  values: T
): { [K in keyof T]: string | undefined } {
  return Object.fromEntries(
    Object.entries(values).map(([clave, valor]) => [clave, textoOpcional(valor)])
  ) as { [K in keyof T]: string | undefined };
}
