import { sampleEstructura } from '../../../test/inventario-fixtures';
import {
  CABECERA_RECEPCION_VACIA,
  TIPOS_DOCUMENTO_PROVEEDOR,
  almacenesDeDestino,
  erroresCabeceraRecepcion,
  puedeRegistrar,
  toRegistrarRecepcionPayload
} from './recepcion-cabecera';
import type { ItemBorrador } from './recepcion-items';

const HOY = '2026-10-08';
const completa = { ...CABECERA_RECEPCION_VACIA, almacenId: 'alm-1' };
const ITEM: ItemBorrador = {
  numeroLineaOrden: 1,
  descripcion: 'Paracetamol 500 mg',
  unidadMedidaCodigo: 'UND',
  cantidadPendiente: 10,
  numeroLote: '',
  fechaFabricacion: '',
  fechaVencimiento: '',
  cantidadRecibida: '',
  cantidadRechazada: '0',
  motivoRechazo: '',
  costoUnitario: '5.5'
};
const itemValido: ItemBorrador = {
  ...ITEM,
  numeroLote: 'L2026-01',
  fechaVencimiento: '2027-12-31',
  cantidadRecibida: '4'
};
const establecimientos = sampleEstructura.companies.flatMap(({ establishments }) => establishments);

describe('cabecera de la recepción', () => {
  it('arranca sin almacén y con factura como documento', () => {
    expect(CABECERA_RECEPCION_VACIA).toEqual({
      almacenId: '',
      documentoProveedorTipo: '01',
      documentoProveedorSerie: '',
      documentoProveedorNumero: '',
      guiaRemisionRemitente: '',
      guiaRemisionTransportista: '',
      temperatura: '',
      humedad: '',
      observacion: ''
    });
    expect(TIPOS_DOCUMENTO_PROVEEDOR).toEqual([
      { codigo: '01', nombre: 'Factura' },
      { codigo: '03', nombre: 'Boleta de venta' }
    ]);
  });
});

describe('almacenesDeDestino', () => {
  it('devuelve los almacenes del establecimiento destino', () => {
    expect(almacenesDeDestino(establecimientos, 'est-1').map(({ id }) => id)).toEqual([
      'alm-1',
      'alm-2'
    ]);
  });

  it('omite los almacenes inactivos', () => {
    const conInactivo = establecimientos.map((establecimiento) => ({
      ...establecimiento,
      warehouses: [
        ...establecimiento.warehouses,
        {
          id: `${establecimiento.id}-cerrado`,
          code: 'ALM999',
          name: 'Almacén Cerrado',
          status: 'INACTIVE' as const
        }
      ]
    }));

    expect(almacenesDeDestino(conInactivo, 'est-2').map(({ id }) => id)).toEqual(['alm-3']);
  });

  it('sin un establecimiento conocido no ofrece almacenes', () => {
    expect(almacenesDeDestino(establecimientos, 'est-x')).toEqual([]);
  });
});

describe('erroresCabeceraRecepcion', () => {
  it('no reporta errores en una cabecera con almacén', () => {
    expect(erroresCabeceraRecepcion(completa)).toEqual({});
  });

  it('exige el almacén', () => {
    expect(erroresCabeceraRecepcion(CABECERA_RECEPCION_VACIA)).toEqual({
      almacenId: 'Selecciona el almacén de recepción.'
    });
  });

  it.each([
    ['documentoProveedorSerie', 20, 'La serie admite hasta 20 caracteres.'],
    ['documentoProveedorNumero', 40, 'El número admite hasta 40 caracteres.'],
    ['guiaRemisionRemitente', 80, 'La guía de remisión admite hasta 80 caracteres.'],
    ['guiaRemisionTransportista', 80, 'La guía de remisión admite hasta 80 caracteres.'],
    ['observacion', 1000, 'La observación admite hasta 1000 caracteres.']
  ] as const)(
    'limita %s a %i caracteres sin contar espacios de los extremos',
    (campo, maximo, mensaje) => {
      expect(erroresCabeceraRecepcion({ ...completa, [campo]: ` ${'a'.repeat(maximo)} ` })).toEqual(
        {}
      );
      expect(erroresCabeceraRecepcion({ ...completa, [campo]: 'a'.repeat(maximo + 1) })).toEqual({
        [campo]: mensaje
      });
    }
  );

  it.each(['-50', '100', '4.25', '-0.5', ' 8 '])('acepta la temperatura "%s"', (temperatura) => {
    expect(erroresCabeceraRecepcion({ ...completa, temperatura })).toEqual({});
  });

  it.each(['-50.01', '100.5', '4.123', 'abc', '1000'])(
    'rechaza la temperatura "%s"',
    (temperatura) => {
      expect(erroresCabeceraRecepcion({ ...completa, temperatura })).toEqual({
        temperatura: 'La temperatura debe estar entre -50 y 100 °C con hasta 2 decimales.'
      });
    }
  );

  it.each(['0', '100', '65.5'])('acepta la humedad "%s"', (humedad) => {
    expect(erroresCabeceraRecepcion({ ...completa, humedad })).toEqual({});
  });

  it.each(['-1', '100.01', '50.123'])('rechaza la humedad "%s"', (humedad) => {
    expect(erroresCabeceraRecepcion({ ...completa, humedad })).toEqual({
      humedad: 'La humedad relativa debe estar entre 0 y 100 % con hasta 2 decimales.'
    });
  });
});

describe('puedeRegistrar', () => {
  it('exige cabecera válida y al menos un ítem recibido sin errores', () => {
    expect(puedeRegistrar(completa, [itemValido], HOY)).toBe(true);
    expect(puedeRegistrar(completa, [itemValido, { ...ITEM, numeroLineaOrden: 2 }], HOY)).toBe(
      true
    );
    expect(puedeRegistrar(CABECERA_RECEPCION_VACIA, [itemValido], HOY)).toBe(false);
    expect(puedeRegistrar(completa, [ITEM], HOY)).toBe(false);
    expect(puedeRegistrar(completa, [{ ...itemValido, numeroLote: '' }], HOY)).toBe(false);
  });
});

describe('toRegistrarRecepcionPayload', () => {
  it('omite los opcionales vacíos y envía solo los ítems recibidos', () => {
    expect(
      toRegistrarRecepcionPayload('orden-1', completa, [
        itemValido,
        { ...ITEM, numeroLineaOrden: 2 }
      ])
    ).toEqual({
      ordenCompraId: 'orden-1',
      almacenId: 'alm-1',
      documentoProveedorTipo: '01',
      documentoProveedorSerie: undefined,
      documentoProveedorNumero: undefined,
      guiaRemisionRemitente: undefined,
      guiaRemisionTransportista: undefined,
      temperaturaRecepcionC: undefined,
      humedadRelativaPct: undefined,
      observacion: undefined,
      items: [
        {
          numeroLineaOrden: 1,
          numeroLote: 'L2026-01',
          fechaFabricacion: undefined,
          fechaVencimiento: '2027-12-31',
          cantidadRecibida: 4,
          cantidadRechazada: 0,
          motivoRechazo: undefined,
          costoUnitario: 5.5
        }
      ]
    });
  });

  it('incluye documento, guías, temperatura, humedad y observación cuando se llenaron', () => {
    expect(
      toRegistrarRecepcionPayload(
        'orden-1',
        {
          ...completa,
          documentoProveedorTipo: '03',
          documentoProveedorSerie: ' B001 ',
          documentoProveedorNumero: ' 98 ',
          guiaRemisionRemitente: ' T001-1 ',
          guiaRemisionTransportista: ' V001-2 ',
          temperatura: ' 4.5 ',
          humedad: '60',
          observacion: ' Cajas completas '
        },
        [itemValido]
      )
    ).toMatchObject({
      documentoProveedorTipo: '03',
      documentoProveedorSerie: 'B001',
      documentoProveedorNumero: '98',
      guiaRemisionRemitente: 'T001-1',
      guiaRemisionTransportista: 'V001-2',
      temperaturaRecepcionC: 4.5,
      humedadRelativaPct: 60,
      observacion: 'Cajas completas'
    });
  });
});
