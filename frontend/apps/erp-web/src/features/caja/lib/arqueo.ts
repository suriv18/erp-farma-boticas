export const diferenciaArqueo = (totalSistema: number, totalDeclarado: number): number =>
  Math.round((totalDeclarado - totalSistema) * 100) / 100;

export const claseDiferencia = (diferencia: number | null): string => {
  if (diferencia === null) return 'text-neutral-900 dark:text-neutral-100';
  if (diferencia < 0) return 'font-semibold text-danger-600 dark:text-danger-400';
  if (diferencia > 0) return 'font-semibold text-warning-700 dark:text-warning-400';
  return 'font-semibold text-success-700 dark:text-success-400';
};
