import { tienePermiso } from '../../features/auth';

const ACCESO_POR_SECCION: Record<string, string | null> = {
  dashboard: null,
  perfil: null,
  catalogo: 'catalogo.',
  inventario: 'inventario.',
  compras: 'compras.',
  ventas: 'ventas.ventas.consultar',
  pos: 'ventas.ventas.registrar',
  caja: 'ventas.turnos.',
  clientes: 'ventas.ventas.',
  seguridad: 'seguridad.',
  organizacion: 'organizacion.'
};

export const seccionDe = (pathname: string): string => pathname.split('/')[1] ?? '';

export function puedeAcceder(permisos: readonly string[], pathname: string): boolean {
  const requerido = ACCESO_POR_SECCION[seccionDe(pathname)];
  return requerido === undefined || requerido === null || tienePermiso(permisos, requerido);
}
