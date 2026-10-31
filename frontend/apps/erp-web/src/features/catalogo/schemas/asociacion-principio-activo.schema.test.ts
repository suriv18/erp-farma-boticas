import {
  asociacionPrincipioActivoSchema,
  type AsociacionPrincipioActivoFormValues
} from './asociacion-principio-activo.schema';

const valido: AsociacionPrincipioActivoFormValues = {
  principioActivoId: 'pa-1',
  concentracionTexto: '',
  cantidad: '',
  unidadMedidaCodigo: '',
  esPrincipal: true,
  orden: '1'
};

function mensajes(valores: Partial<AsociacionPrincipioActivoFormValues>) {
  const resultado = asociacionPrincipioActivoSchema.safeParse({ ...valido, ...valores });
  return resultado.success ? [] : resultado.error.issues.map((issue) => issue.message);
}

describe('asociacionPrincipioActivoSchema', () => {
  it('acepta una asociacion minima y una completa', () => {
    expect(mensajes({})).toEqual([]);
    expect(
      mensajes({
        concentracionTexto: '500 mg',
        cantidad: '500.5',
        unidadMedidaCodigo: 'MG',
        orden: '0'
      })
    ).toEqual([]);
  });

  it.each([
    [{ principioActivoId: '' }, 'Selecciona un principio activo.'],
    [{ concentracionTexto: 'x'.repeat(201) }, 'La concentración no debe exceder 200 caracteres.'],
    [{ cantidad: '0' }, 'La cantidad debe ser un número mayor que cero con hasta 6 decimales.'],
    [
      { cantidad: '1.1234567' },
      'La cantidad debe ser un número mayor que cero con hasta 6 decimales.'
    ],
    [{ orden: '' }, 'El orden debe ser un entero entre 0 y 32767.'],
    [{ orden: '1.5' }, 'El orden debe ser un entero entre 0 y 32767.'],
    [{ orden: '32768' }, 'El orden debe ser un entero entre 0 y 32767.']
  ])('rechaza %j', (valores, mensaje) => {
    expect(mensajes(valores)).toEqual([mensaje]);
  });
});
