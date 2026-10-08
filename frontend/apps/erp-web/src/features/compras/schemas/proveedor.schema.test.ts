import { PROVEEDOR_FORM_VACIO } from '../lib/proveedor-form';
import { proveedorSchema } from './proveedor.schema';

const valido = { ...PROVEEDOR_FORM_VACIO, numeroDocumento: '20100070970', razonSocial: 'Lab SAC' };

const mensajeDe = (campo: string, valores: object) => {
  const resultado = proveedorSchema.safeParse(valores);
  return resultado.success
    ? undefined
    : resultado.error.issues.find((issue) => issue.path[0] === campo)?.message;
};

describe('proveedorSchema', () => {
  it('acepta un proveedor con los valores por defecto y un RUC válido', () => {
    expect(proveedorSchema.safeParse(valido).success).toBe(true);
  });

  it.each(['6', ''])('con tipo "%s" exige un RUC de 11 dígitos que empiece con 10 o 20', (tipo) => {
    const mensaje = 'El RUC debe tener 11 dígitos y empezar con 10 o 20.';

    expect(mensajeDe('numeroDocumento', { ...valido, tipoDocumento: tipo, numeroDocumento: '30100070970' })).toBe(mensaje);
    expect(mensajeDe('numeroDocumento', { ...valido, tipoDocumento: tipo, numeroDocumento: '2010007097' })).toBe(mensaje);
    expect(mensajeDe('numeroDocumento', { ...valido, tipoDocumento: tipo, numeroDocumento: '10100070970' })).toBeUndefined();
  });

  it('con otro tipo de documento no exige formato de RUC', () => {
    expect(
      proveedorSchema.safeParse({ ...valido, tipoDocumento: '1', numeroDocumento: '12345678' }).success
    ).toBe(true);
  });

  it('valida los límites de documento, razón social y textos', () => {
    expect(mensajeDe('tipoDocumento', { ...valido, tipoDocumento: '123' })).toBe('El tipo de documento admite hasta 2 caracteres.');
    expect(mensajeDe('numeroDocumento', { ...valido, numeroDocumento: '' })).toBe('El número de documento es obligatorio.');
    expect(mensajeDe('numeroDocumento', { ...valido, tipoDocumento: '1', numeroDocumento: '1'.repeat(16) })).toBe('El número de documento admite hasta 15 caracteres.');
    expect(mensajeDe('razonSocial', { ...valido, razonSocial: '   ' })).toBe('La razón social es obligatoria.');
    expect(mensajeDe('razonSocial', { ...valido, razonSocial: 'a'.repeat(301) })).toBe('La razón social no debe exceder 300 caracteres.');
    expect(mensajeDe('nombreComercial', { ...valido, nombreComercial: 'a'.repeat(301) })).toBe('El nombre comercial no debe exceder 300 caracteres.');
    expect(mensajeDe('direccion', { ...valido, direccion: 'a'.repeat(501) })).toBe('La dirección no debe exceder 500 caracteres.');
    expect(mensajeDe('contactoNombre', { ...valido, contactoNombre: 'a'.repeat(181) })).toBe('El contacto no debe exceder 180 caracteres.');
    expect(mensajeDe('condicionPagoDefault', { ...valido, condicionPagoDefault: 'a'.repeat(81) })).toBe('La condición de pago no debe exceder 80 caracteres.');
    expect(mensajeDe('calificacion', { ...valido, calificacion: 'a'.repeat(31) })).toBe('La calificación no debe exceder 30 caracteres.');
  });

  it('valida ubigeo, teléfonos y correos', () => {
    expect(mensajeDe('ubigeo', { ...valido, ubigeo: '1501' })).toBe('El ubigeo debe tener 6 dígitos.');
    expect(mensajeDe('ubigeo', { ...valido, ubigeo: '150101' })).toBeUndefined();
    expect(mensajeDe('telefono', { ...valido, telefono: 'abc' })).toContain('El teléfono');
    expect(mensajeDe('contactoTelefono', { ...valido, contactoTelefono: '999 888 777' })).toBeUndefined();
    expect(mensajeDe('email', { ...valido, email: 'sin-arroba' })).toBe('El correo no es válido.');
    expect(mensajeDe('email', { ...valido, email: `${'a'.repeat(250)}@x.pe` })).toBe('El correo no debe exceder 254 caracteres.');
    expect(mensajeDe('contactoEmail', { ...valido, contactoEmail: 'ventas@lab.pe' })).toBeUndefined();
    expect(mensajeDe('contactoEmail', { ...valido, contactoEmail: `${'a'.repeat(250)}@x.pe` })).toBe('El correo no debe exceder 254 caracteres.');
  });

  it('valida los días de crédito y la moneda', () => {
    const mensajeDias = 'Los días de crédito deben ser un entero mayor o igual a 0.';

    expect(mensajeDe('diasCreditoDefault', { ...valido, diasCreditoDefault: '-1' })).toBe(mensajeDias);
    expect(mensajeDe('diasCreditoDefault', { ...valido, diasCreditoDefault: '1.5' })).toBe(mensajeDias);
    expect(mensajeDe('diasCreditoDefault', { ...valido, diasCreditoDefault: '' })).toBe(mensajeDias);
    expect(mensajeDe('diasCreditoDefault', { ...valido, diasCreditoDefault: '45' })).toBeUndefined();
    expect(mensajeDe('monedaDefault', { ...valido, monedaDefault: 'pen' })).toBe('La moneda debe ser un código de 3 letras mayúsculas.');
    expect(mensajeDe('monedaDefault', { ...valido, monedaDefault: 'USD' })).toBeUndefined();
  });
});
