import { ASOCIACION_PRINCIPIO_ACTIVO_VACIA, toAsociarPayload } from './asociacion-principio-activo';

describe('asociacion-principio-activo', () => {
  it('toAsociarPayload omite los opcionales vacios y convierte el orden', () => {
    expect(
      toAsociarPayload({
        ...ASOCIACION_PRINCIPIO_ACTIVO_VACIA,
        principioActivoId: 'pa-1',
        orden: '2'
      })
    ).toEqual({
      principioActivoId: 'pa-1',
      concentracionTexto: undefined,
      cantidad: undefined,
      unidadMedidaCodigo: undefined,
      esPrincipal: true,
      orden: 2
    });
  });

  it('toAsociarPayload convierte cantidad y recorta textos', () => {
    expect(
      toAsociarPayload({
        principioActivoId: 'pa-1',
        concentracionTexto: ' 500 mg ',
        cantidad: '500.5',
        unidadMedidaCodigo: 'MG',
        esPrincipal: true,
        orden: '1'
      })
    ).toEqual({
      principioActivoId: 'pa-1',
      concentracionTexto: '500 mg',
      cantidad: 500.5,
      unidadMedidaCodigo: 'MG',
      esPrincipal: true,
      orden: 1
    });
  });
});
