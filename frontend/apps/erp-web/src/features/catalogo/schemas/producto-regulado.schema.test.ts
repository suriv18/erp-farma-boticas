import {
  productoReguladoSchema,
  type ProductoReguladoFormValues
} from './producto-regulado.schema';

const valido: ProductoReguladoFormValues = {
  tipoProducto: 'FARMACEUTICO',
  rubroCodigo: '',
  tipoRegistro: '',
  numeroRegistro: '',
  denominacion: 'Paracetamol 500 mg',
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

function mensajes(valores: Partial<ProductoReguladoFormValues>) {
  const resultado = productoReguladoSchema.safeParse({ ...valido, ...valores });
  return resultado.success ? [] : resultado.error.issues.map((issue) => issue.message);
}

describe('productoReguladoSchema', () => {
  it('acepta un producto con solo los campos obligatorios', () => {
    expect(mensajes({})).toEqual([]);
  });

  it.each([
    [{ tipoProducto: 'x' }, 'El tipo de producto debe tener al menos 2 caracteres.'],
    [{ tipoProducto: 'x'.repeat(41) }, 'El tipo de producto no debe exceder 40 caracteres.'],
    [{ denominacion: 'x' }, 'La denominación debe tener al menos 2 caracteres.'],
    [{ denominacion: 'x'.repeat(501) }, 'La denominación no debe exceder 500 caracteres.'],
    [{ rubroCodigo: 'x'.repeat(51) }, 'El rubro no debe exceder 50 caracteres.'],
    [
      { tipoRegistro: 'x'.repeat(41), numeroRegistro: '1' },
      'El tipo de registro no debe exceder 40 caracteres.'
    ],
    [{ concentracionTexto: 'x'.repeat(301) }, 'La concentración no debe exceder 300 caracteres.'],
    [
      { presentacionRegulatoria: 'x'.repeat(501) },
      'La presentación regulatoria no debe exceder 500 caracteres.'
    ],
    [{ clasificacionAtc: 'x'.repeat(31) }, 'La clasificación ATC no debe exceder 30 caracteres.'],
    [{ paisOrigen: 'x'.repeat(101) }, 'El país de origen no debe exceder 100 caracteres.'],
    [
      { establecimientoExpendio: 'x'.repeat(201) },
      'El establecimiento de expendio no debe exceder 200 caracteres.'
    ],
    [{ versionFuente: 'x'.repeat(101) }, 'La versión de fuente no debe exceder 100 caracteres.'],
    [{ vigenteDesde: '01/10/2026' }, 'Ingresa una fecha válida.']
  ])('rechaza %j', (valores, mensaje) => {
    expect(mensajes(valores)).toContain(mensaje);
  });

  it('exige informar juntos el tipo y el numero de registro', () => {
    expect(mensajes({ tipoRegistro: 'RS' })).toEqual([
      'El tipo y el número de registro deben informarse juntos.'
    ]);
    expect(mensajes({ numeroRegistro: 'EE-1' })).toEqual([
      'El tipo y el número de registro deben informarse juntos.'
    ]);
    expect(mensajes({ tipoRegistro: 'RS', numeroRegistro: 'EE-1' })).toEqual([]);
  });

  it('no permite una vigencia final anterior a la inicial', () => {
    expect(mensajes({ vigenteDesde: '2026-10-01', vigenteHasta: '2026-09-30' })).toEqual([
      'La vigencia hasta no puede ser anterior a la vigencia desde.'
    ]);
    expect(mensajes({ vigenteDesde: '2026-10-01', vigenteHasta: '2026-10-01' })).toEqual([]);
    expect(mensajes({ vigenteHasta: '2026-09-30' })).toEqual([]);
  });
});
