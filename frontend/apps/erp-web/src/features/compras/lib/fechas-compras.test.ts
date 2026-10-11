import { fechaUtcISO } from './fecha-utc';

describe('fechaUtcISO', () => {
  it('usa la fecha UTC aunque la hora local ya sea del día siguiente o anterior', () => {
    expect(fechaUtcISO(new Date('2026-10-08T23:30:00-05:00'))).toBe('2026-10-09');
    expect(fechaUtcISO(new Date('2026-10-08T01:00:00+05:00'))).toBe('2026-10-07');
  });

  it('usa la fecha de hoy por defecto', () => {
    expect(fechaUtcISO()).toBe(fechaUtcISO(new Date()));
  });
});
