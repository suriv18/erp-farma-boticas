import { sampleEstructura } from '../../../test/inventario-fixtures';
import { almacenesDe, opcionesEstablecimientos } from './estructura';

describe('estructura', () => {
  it('opcionesEstablecimientos devuelve una lista vacía sin estructura', () => {
    expect(opcionesEstablecimientos(undefined)).toEqual([]);
  });

  it('opcionesEstablecimientos aplana empresas, establecimientos y almacenes', () => {
    expect(opcionesEstablecimientos(sampleEstructura)).toEqual([
      {
        id: 'est-1',
        nombre: 'Botica Central',
        almacenes: [
          { id: 'alm-1', nombre: 'Almacén Central', establecimiento: 'Botica Central' },
          { id: 'alm-2', nombre: 'Almacén Frío', establecimiento: 'Botica Central' }
        ]
      },
      {
        id: 'est-2',
        nombre: 'Botica Norte',
        almacenes: [{ id: 'alm-3', nombre: 'Almacén Norte', establecimiento: 'Botica Norte' }]
      }
    ]);
  });

  it('almacenesDe devuelve todos los almacenes sin establecimiento', () => {
    const opciones = opcionesEstablecimientos(sampleEstructura);

    expect(almacenesDe(opciones, undefined).map(({ id }) => id)).toEqual([
      'alm-1',
      'alm-2',
      'alm-3'
    ]);
    expect(almacenesDe(opciones, '').map(({ id }) => id)).toEqual(['alm-1', 'alm-2', 'alm-3']);
  });

  it('almacenesDe filtra por establecimiento', () => {
    const opciones = opcionesEstablecimientos(sampleEstructura);

    expect(almacenesDe(opciones, 'est-2').map(({ id }) => id)).toEqual(['alm-3']);
  });
});
