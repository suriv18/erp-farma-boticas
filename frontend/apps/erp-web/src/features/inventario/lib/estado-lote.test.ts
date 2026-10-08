import { ESTADOS_LOTE, type EstadoLote } from '../api/inventario.types';
import { puedeBloquear, puedeDesbloquear, tonoEstadoLote } from './estado-lote';

describe('estado-lote', () => {
  it.each<[EstadoLote, string]>([
    ['HABILITADO', 'success'],
    ['CUARENTENA', 'warning'],
    ['BLOQUEADO', 'danger'],
    ['INMOVILIZADO_RECALL', 'danger'],
    ['VENCIDO', 'neutral'],
    ['BAJA_DESTRUIDO', 'neutral']
  ])('el tono de %s es %s', (estado, tono) => {
    expect(tonoEstadoLote(estado)).toBe(tono);
  });

  it.each(ESTADOS_LOTE)(
    'puedeBloquear(%s) solo es verdadero para habilitado y cuarentena',
    (estado) => {
      expect(puedeBloquear(estado)).toBe(estado === 'HABILITADO' || estado === 'CUARENTENA');
    }
  );

  it.each(ESTADOS_LOTE)('puedeDesbloquear(%s) solo es verdadero para bloqueado', (estado) => {
    expect(puedeDesbloquear(estado)).toBe(estado === 'BLOQUEADO');
  });
});
