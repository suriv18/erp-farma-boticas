import { hrefContacto } from './contacto';

describe('hrefContacto', () => {
  it('construye mailto para un correo', () => {
    expect(hrefContacto('correo', ' contacto@boticas.pe ')).toBe('mailto:contacto@boticas.pe');
  });

  it('no enlaza un correo sin arroba', () => {
    expect(hrefContacto('correo', 'no-es-correo')).toBeNull();
  });

  it('construye tel con solo dígitos y +', () => {
    expect(hrefContacto('telefono', '+51 (1) 444-5566')).toBe('tel:+5114445566');
  });

  it('no enlaza un teléfono con menos de 6 dígitos', () => {
    expect(hrefContacto('telefono', 'abc')).toBeNull();
  });

  it('enlaza un sitio web http o https tal cual', () => {
    expect(hrefContacto('web', 'https://boticas.pe')).toBe('https://boticas.pe');
    expect(hrefContacto('web', 'http://boticas.pe')).toBe('http://boticas.pe');
  });

  it('no enlaza un sitio web sin protocolo http(s)', () => {
    expect(hrefContacto('web', 'x')).toBeNull();
    expect(hrefContacto('web', 'javascript:alert(1)')).toBeNull();
  });
});
