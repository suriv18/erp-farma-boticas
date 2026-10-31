import { sampleProveedor } from '../../../test/compras-fixtures';
import { PROVEEDOR_FORM_VACIO, proveedorAFormulario, toProveedorPayload } from './proveedor-form';

describe('proveedor-form', () => {
  it('el formulario vacío trae los valores por defecto del backend', () => {
    expect(PROVEEDOR_FORM_VACIO).toEqual({
      tipoDocumento: '6',
      numeroDocumento: '',
      razonSocial: '',
      nombreComercial: '',
      direccion: '',
      ubigeo: '',
      telefono: '',
      email: '',
      contactoNombre: '',
      contactoTelefono: '',
      contactoEmail: '',
      condicionPagoDefault: 'CONTADO',
      diasCreditoDefault: '0',
      monedaDefault: 'PEN',
      calificacion: 'CONFIABLE',
      esLaboratorio: false,
      esImportador: false,
      esDistribuidor: true
    });
  });

  it('proveedorAFormulario convierte nulos en vacíos y días en texto', () => {
    expect(proveedorAFormulario(sampleProveedor)).toEqual({
      ...PROVEEDOR_FORM_VACIO,
      numeroDocumento: '20100070970',
      razonSocial: 'Laboratorios Perú SAC',
      nombreComercial: 'LabPerú',
      condicionPagoDefault: 'CREDITO 30',
      diasCreditoDefault: '30',
      esLaboratorio: true,
      esDistribuidor: true
    });
  });

  it('proveedorAFormulario usa vacío cuando el proveedor no trae tipo, condición, moneda ni calificación', () => {
    const formulario = proveedorAFormulario({
      ...sampleProveedor,
      tipoDocumento: null,
      condicionPagoDefault: null,
      monedaDefault: null,
      calificacion: null
    });

    expect(formulario.tipoDocumento).toBe('');
    expect(formulario.condicionPagoDefault).toBe('');
    expect(formulario.monedaDefault).toBe('');
    expect(formulario.calificacion).toBe('');
  });

  it('toProveedorPayload recorta, omite los textos vacíos y convierte los días a número', () => {
    const payload = toProveedorPayload({
      ...PROVEEDOR_FORM_VACIO,
      numeroDocumento: ' 20100070970 ',
      razonSocial: '  Lab SAC ',
      email: ' ventas@lab.pe ',
      diasCreditoDefault: '45',
      esLaboratorio: true
    });

    expect(payload).toEqual({
      tipoDocumento: '6',
      numeroDocumento: '20100070970',
      razonSocial: 'Lab SAC',
      nombreComercial: undefined,
      direccion: undefined,
      ubigeo: undefined,
      telefono: undefined,
      email: 'ventas@lab.pe',
      contactoNombre: undefined,
      contactoTelefono: undefined,
      contactoEmail: undefined,
      condicionPagoDefault: 'CONTADO',
      diasCreditoDefault: 45,
      monedaDefault: 'PEN',
      esLaboratorio: true,
      esImportador: false,
      esDistribuidor: true,
      calificacion: 'CONFIABLE'
    });
  });
});
