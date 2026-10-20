import { describe, expect, it } from 'vitest';
import { tienePermiso } from './permisos';

describe('tienePermiso', () => {
  const permisos = ['ventas.turnos.abrir', 'catalogo.skus.consultar'];

  it('acepta una coincidencia exacta', () => {
    expect(tienePermiso(permisos, 'ventas.turnos.abrir')).toBe(true);
  });

  it('rechaza un permiso que no se tiene', () => {
    expect(tienePermiso(permisos, 'ventas.turnos.cerrar')).toBe(false);
  });

  it('trata un requerido terminado en punto como prefijo', () => {
    expect(tienePermiso(permisos, 'ventas.')).toBe(true);
    expect(tienePermiso(permisos, 'seguridad.')).toBe(false);
  });

  it('no concede un prefijo a quien no tiene permisos', () => {
    expect(tienePermiso([], 'ventas.')).toBe(false);
  });
});
