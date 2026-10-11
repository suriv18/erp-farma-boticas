import type { ProductoRegulado } from '../api/productos-regulados.types';
import {
  PRODUCTO_REGULADO_VACIO,
  toProductoFormValues,
  toProductoPayload
} from './producto-regulado-form';

const producto: ProductoRegulado = {
  id: 'pr-1',
  tipoProducto: 'FARMACEUTICO',
  rubroCodigo: null,
  tipoRegistro: 'RS',
  numeroRegistro: 'EE-100',
  denominacion: 'Paracetamol 500 mg',
  concentracionTexto: '500 mg',
  presentacionRegulatoria: null,
  formaFarmaceuticaCodigo: 'TAB',
  viaAdministracionCodigo: 'ORAL',
  unidadMedidaCodigo: null,
  condicionVentaCodigo: 'OTC',
  clasificacionAtc: null,
  clasificacionControladaCodigo: null,
  tipoLiberacion: null,
  origenFabricacion: null,
  paisOrigen: 'Perú',
  subpartidaNacional: null,
  titularRegistro: null,
  fabricante: 'Farmalab',
  importador: null,
  establecimientoExpendio: null,
  vigenteDesde: '2026-01-01',
  vigenteHasta: null,
  fuente: 'DIGEMID',
  versionFuente: null,
  principiosActivos: [],
  estadoRegulatorio: 'VIGENTE',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null
};

describe('producto-regulado-form', () => {
  it('toProductoFormValues convierte nulos en cadenas vacias y conserva los valores', () => {
    const values = toProductoFormValues(producto);

    expect(values).toEqual({
      ...PRODUCTO_REGULADO_VACIO,
      tipoProducto: 'FARMACEUTICO',
      tipoRegistro: 'RS',
      numeroRegistro: 'EE-100',
      denominacion: 'Paracetamol 500 mg',
      concentracionTexto: '500 mg',
      formaFarmaceuticaCodigo: 'TAB',
      viaAdministracionCodigo: 'ORAL',
      condicionVentaCodigo: 'OTC',
      paisOrigen: 'Perú',
      fabricante: 'Farmalab',
      vigenteDesde: '2026-01-01',
      fuente: 'DIGEMID'
    });
  });

  it('toProductoPayload omite los campos opcionales vacios', () => {
    const payload = toProductoPayload({
      ...PRODUCTO_REGULADO_VACIO,
      tipoProducto: 'FARMACEUTICO',
      denominacion: 'Paracetamol',
      fabricante: ' Farmalab '
    });

    expect(payload.tipoProducto).toBe('FARMACEUTICO');
    expect(payload.denominacion).toBe('Paracetamol');
    expect(payload.fabricante).toBe('Farmalab');
    expect(payload.rubroCodigo).toBeUndefined();
    expect(payload.vigenteDesde).toBeUndefined();
  });
});
