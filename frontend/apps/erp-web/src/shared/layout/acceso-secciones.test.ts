import { describe, expect, it } from 'vitest';
import { puedeAcceder, seccionDe } from './acceso-secciones';

describe('acceso por sección', () => {
  const cajero = ['ventas.turnos.abrir', 'ventas.ventas.registrar', 'ventas.ventas.consultar'];

  it('obtiene la sección del primer segmento de la ruta', () => {
    expect(seccionDe('/caja')).toBe('caja');
    expect(seccionDe('/seguridad/usuarios/1')).toBe('seguridad');
    expect(seccionDe('/')).toBe('');
    expect(seccionDe('')).toBe('');
  });

  it('permite sin permisos el resumen y el perfil', () => {
    expect(puedeAcceder([], '/dashboard')).toBe(true);
    expect(puedeAcceder([], '/perfil')).toBe(true);
  });

  it('permite una ruta desconocida para que responda la página 404', () => {
    expect(puedeAcceder([], '/no-existe')).toBe(true);
  });

  it('un cajero accede a caja, POS y ventas pero no a seguridad ni organización', () => {
    expect(puedeAcceder(cajero, '/caja')).toBe(true);
    expect(puedeAcceder(cajero, '/pos')).toBe(true);
    expect(puedeAcceder(cajero, '/ventas')).toBe(true);
    expect(puedeAcceder(cajero, '/seguridad')).toBe(false);
    expect(puedeAcceder(cajero, '/organizacion/empresas')).toBe(false);
  });
});
