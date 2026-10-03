import { codigoBarraSchema, type CodigoBarraFormValues } from './codigo-barra.schema';

const valido: CodigoBarraFormValues = {
  codigoBarra: '7750001000012',
  tipoCodigo: 'EAN13',
  vigenteDesde: '',
  vigenteHasta: ''
};

function mensajes(valores: Partial<CodigoBarraFormValues>) {
  const resultado = codigoBarraSchema.safeParse({ ...valido, ...valores });
  return resultado.success ? [] : resultado.error.issues.map((issue) => issue.message);
}

describe('codigoBarraSchema', () => {
  it('acepta un codigo de barras valido', () => {
    expect(mensajes({})).toEqual([]);
  });

  it.each([
    [{ codigoBarra: '  ' }, 'El código de barras es obligatorio.'],
    [{ codigoBarra: '1'.repeat(81) }, 'El código de barras no debe exceder 80 caracteres.'],
    [{ tipoCodigo: 'x'.repeat(31) }, 'El tipo de código no debe exceder 30 caracteres.'],
    [{ vigenteDesde: '01/10/2026' }, 'Ingresa una fecha válida.']
  ])('rechaza %j', (valores, mensaje) => {
    expect(mensajes(valores)).toContain(mensaje);
  });

  it('no permite una vigencia final anterior a la inicial', () => {
    expect(mensajes({ vigenteDesde: '2026-10-01', vigenteHasta: '2026-09-01' })).toEqual([
      'La vigencia hasta no puede ser anterior a la vigencia desde.'
    ]);
    expect(mensajes({ vigenteDesde: '2026-10-01', vigenteHasta: '2026-10-01' })).toEqual([]);
    expect(mensajes({ vigenteHasta: '2026-09-01' })).toEqual([]);
  });
});
