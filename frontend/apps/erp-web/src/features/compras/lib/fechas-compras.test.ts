import { fechaLocalISO, fechaUtcISO } from './fechas-compras';

describe('fechaUtcISO', () => {
  it('usa la fecha UTC aunque la hora local ya sea del día siguiente o anterior', () => {
    expect(fechaUtcISO(new Date('2026-10-08T23:30:00-05:00'))).toBe('2026-10-09');
    expect(fechaUtcISO(new Date('2026-10-08T01:00:00+05:00'))).toBe('2026-10-07');
  });

  it('usa la fecha de hoy por defecto', () => {
    expect(fechaUtcISO()).toBe(fechaUtcISO(new Date()));
  });
});

describe('fechaLocalISO', () => {
  it('usa la fecha local con ceros a la izquierda', () => {
    expect(fechaLocalISO(new Date(2026, 0, 5))).toBe('2026-01-05');
    expect(fechaLocalISO(new Date(2026, 9, 6))).toBe('2026-10-06');
  });

  it('usa la fecha de hoy por defecto', () => {
    expect(fechaLocalISO()).toBe(fechaLocalISO(new Date()));
  });
});
