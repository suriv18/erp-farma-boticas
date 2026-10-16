export const fechaUtcISO = (ahora: Date = new Date()): string => ahora.toISOString().slice(0, 10);

export function fechaLocalISO(ahora: Date = new Date()): string {
  const mes = String(ahora.getMonth() + 1).padStart(2, '0');
  const dia = String(ahora.getDate()).padStart(2, '0');
  return `${ahora.getFullYear()}-${mes}-${dia}`;
}
