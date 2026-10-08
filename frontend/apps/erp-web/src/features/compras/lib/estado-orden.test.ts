import { ESTADOS_ORDEN } from '../api/ordenes.types';
import {
  etiquetaEstadoOrden,
  puedeAnular,
  puedeAprobar,
  puedeEmitir,
  puedeRecibir,
  tonoEstadoOrden
} from './estado-orden';

describe('estado-orden', () => {
  it('etiqueta y tono de cada estado', () => {
    expect(ESTADOS_ORDEN.map(etiquetaEstadoOrden)).toEqual([
      'Borrador',
      'En aprobación',
      'Aprobada',
      'Emitida',
      'Parcialmente recibida',
      'Recibida',
      'Cancelada',
      'Cerrada'
    ]);
    expect(ESTADOS_ORDEN.map(tonoEstadoOrden)).toEqual([
      'neutral',
      'warning',
      'success',
      'success',
      'warning',
      'success',
      'danger',
      'neutral'
    ]);
  });

  it('aprobar solo en borrador o en aprobación', () => {
    expect(ESTADOS_ORDEN.filter(puedeAprobar)).toEqual(['BORRADOR', 'EN_APROBACION']);
  });

  it('emitir solo cuando está aprobada', () => {
    expect(ESTADOS_ORDEN.filter(puedeEmitir)).toEqual(['APROBADA']);
  });

  it('anular hasta antes de recibir mercadería', () => {
    expect(ESTADOS_ORDEN.filter(puedeAnular)).toEqual([
      'BORRADOR',
      'EN_APROBACION',
      'APROBADA',
      'EMITIDA'
    ]);
  });

  it('recibir solo cuando está emitida o parcialmente recibida', () => {
    expect(ESTADOS_ORDEN.filter(puedeRecibir)).toEqual(['EMITIDA', 'PARCIALMENTE_RECIBIDA']);
  });
});
