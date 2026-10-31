const instante = (fecha: string, hora: [number, number, number, number]): string => {
  const [anio = 0, mes = 1, dia = 1] = fecha.split('-').map(Number);
  return new Date(anio, mes - 1, dia, ...hora).toISOString();
};

export const inicioDelDia = (fecha: string): string => instante(fecha, [0, 0, 0, 0]);

export const finDelDia = (fecha: string): string => instante(fecha, [23, 59, 59, 999]);
