import { sampleEstructura } from '../../../test/inventario-fixtures';
import { almacenesDeEstablecimiento } from './almacenes-de-establecimiento';

const establecimientos = sampleEstructura.companies.flatMap(({ establishments }) => establishments);

describe('almacenesDeEstablecimiento', () => {
  it('devuelve los almacenes del establecimiento indicado', () => {
    expect(almacenesDeEstablecimiento(establecimientos, 'est-1').map(({ id }) => id)).toEqual([
      'alm-1',
      'alm-2'
    ]);
  });

  it.each(['', 'desconocido'])('devuelve una lista vacía para el establecimiento "%s"', (id) => {
    expect(almacenesDeEstablecimiento(establecimientos, id)).toEqual([]);
  });
});
