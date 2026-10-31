import { emptyToUndefined, orEmpty } from '../../../shared/lib/form-values';
import type { Proveedor, ProveedorPayload } from '../api/proveedores.types';
import type { ProveedorFormValues } from '../schemas/proveedor.schema';

export const PROVEEDOR_FORM_VACIO: ProveedorFormValues = {
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
};

export const proveedorAFormulario = (proveedor: Proveedor): ProveedorFormValues => ({
  tipoDocumento: orEmpty(proveedor.tipoDocumento),
  numeroDocumento: proveedor.numeroDocumento,
  razonSocial: proveedor.razonSocial,
  nombreComercial: orEmpty(proveedor.nombreComercial),
  direccion: orEmpty(proveedor.direccion),
  ubigeo: orEmpty(proveedor.ubigeo),
  telefono: orEmpty(proveedor.telefono),
  email: orEmpty(proveedor.email),
  contactoNombre: orEmpty(proveedor.contactoNombre),
  contactoTelefono: orEmpty(proveedor.contactoTelefono),
  contactoEmail: orEmpty(proveedor.contactoEmail),
  condicionPagoDefault: orEmpty(proveedor.condicionPagoDefault),
  diasCreditoDefault: String(proveedor.diasCreditoDefault),
  monedaDefault: orEmpty(proveedor.monedaDefault),
  calificacion: orEmpty(proveedor.calificacion),
  esLaboratorio: proveedor.esLaboratorio,
  esImportador: proveedor.esImportador,
  esDistribuidor: proveedor.esDistribuidor
});

export const toProveedorPayload = (values: ProveedorFormValues): ProveedorPayload => ({
  tipoDocumento: emptyToUndefined(values.tipoDocumento),
  numeroDocumento: values.numeroDocumento.trim(),
  razonSocial: values.razonSocial.trim(),
  nombreComercial: emptyToUndefined(values.nombreComercial),
  direccion: emptyToUndefined(values.direccion),
  ubigeo: emptyToUndefined(values.ubigeo),
  telefono: emptyToUndefined(values.telefono),
  email: emptyToUndefined(values.email),
  contactoNombre: emptyToUndefined(values.contactoNombre),
  contactoTelefono: emptyToUndefined(values.contactoTelefono),
  contactoEmail: emptyToUndefined(values.contactoEmail),
  condicionPagoDefault: emptyToUndefined(values.condicionPagoDefault),
  diasCreditoDefault: Number(values.diasCreditoDefault),
  monedaDefault: values.monedaDefault,
  esLaboratorio: values.esLaboratorio,
  esImportador: values.esImportador,
  esDistribuidor: values.esDistribuidor,
  calificacion: emptyToUndefined(values.calificacion)
});
