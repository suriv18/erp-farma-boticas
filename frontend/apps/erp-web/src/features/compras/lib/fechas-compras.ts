export const fechaUtcISO = (ahora: Date = new Date()): string => ahora.toISOString().slice(0, 10);
