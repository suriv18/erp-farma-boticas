import type { ProductoRegulado, ProductoReguladoPayload } from '../api/productos-regulados.types';
import type { ProductoReguladoFormValues } from '../schemas/producto-regulado.schema';
import { aOpcionales, textoDeFormulario } from './form-values';

export const PRODUCTO_REGULADO_VACIO: ProductoReguladoFormValues = {
  tipoProducto: '',
  rubroCodigo: '',
  tipoRegistro: '',
  numeroRegistro: '',
  denominacion: '',
  concentracionTexto: '',
  presentacionRegulatoria: '',
  formaFarmaceuticaCodigo: '',
  viaAdministracionCodigo: '',
  unidadMedidaCodigo: '',
  condicionVentaCodigo: '',
  clasificacionAtc: '',
  clasificacionControladaCodigo: '',
  tipoLiberacion: '',
  origenFabricacion: '',
  paisOrigen: '',
  subpartidaNacional: '',
  titularRegistro: '',
  fabricante: '',
  importador: '',
  establecimientoExpendio: '',
  vigenteDesde: '',
  vigenteHasta: '',
  fuente: '',
  versionFuente: ''
};

const CLAVES = Object.keys(PRODUCTO_REGULADO_VACIO) as (keyof ProductoReguladoFormValues)[];

export function toProductoPayload(values: ProductoReguladoFormValues): ProductoReguladoPayload {
  const { tipoProducto, denominacion, ...resto } = values;
  return { tipoProducto, denominacion, ...aOpcionales(resto) };
}

export function toProductoFormValues(producto: ProductoRegulado): ProductoReguladoFormValues {
  return Object.fromEntries(
    CLAVES.map((clave) => [clave, textoDeFormulario(producto[clave])])
  ) as ProductoReguladoFormValues;
}
