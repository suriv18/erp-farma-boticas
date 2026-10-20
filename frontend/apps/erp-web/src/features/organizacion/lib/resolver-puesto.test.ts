import { describe, expect, it } from 'vitest';
import { sampleMiPuesto } from '../../../test/mi-puesto-fixtures';
import type { MiPuestoEstablecimiento } from '../api/mi-puesto.types';
import { resolverPuesto } from './resolver-puesto';

const [central, norte] = sampleMiPuesto.establecimientos as [
  MiPuestoEstablecimiento,
  MiPuestoEstablecimiento
];
const conUnaTerminal: MiPuestoEstablecimiento = { ...central, terminales: [central.terminales[0]!] };
const sinOperables: MiPuestoEstablecimiento = { ...norte };

describe('resolverPuesto', () => {
  it('no cambia nada si hay varios establecimientos y ninguno elegido', () => {
    expect(resolverPuesto([central, norte], { establecimientoId: '', terminalId: '' })).toBeNull();
  });

  it('elige el único establecimiento y su única terminal operable', () => {
    expect(resolverPuesto([conUnaTerminal], { establecimientoId: '', terminalId: '' })).toEqual({
      establecimientoId: 'est-1',
      terminalId: 'term-1'
    });
  });

  it('elige el único establecimiento sin terminal cuando ninguna es operable', () => {
    expect(resolverPuesto([sinOperables], { establecimientoId: '', terminalId: '' })).toEqual({
      establecimientoId: 'est-2',
      terminalId: ''
    });
  });

  it('descarta un establecimiento recordado que ya no está en el alcance', () => {
    expect(resolverPuesto([central], { establecimientoId: 'est-x', terminalId: 'term-1' })).toEqual({
      establecimientoId: '',
      terminalId: ''
    });
  });

  it('no hace nada si no hay establecimiento elegido y no hay ninguno disponible', () => {
    expect(resolverPuesto([], { establecimientoId: '', terminalId: '' })).toBeNull();
  });

  it('descarta una terminal recordada que ya no es operable', () => {
    expect(resolverPuesto([sinOperables], { establecimientoId: 'est-2', terminalId: 'term-3' })).toEqual({
      establecimientoId: 'est-2',
      terminalId: ''
    });
  });

  it('conserva la terminal recordada cuando sigue siendo operable', () => {
    expect(resolverPuesto([central], { establecimientoId: 'est-1', terminalId: 'term-2' })).toBeNull();
  });

  it('elige la única terminal operable del establecimiento recordado', () => {
    expect(resolverPuesto([conUnaTerminal], { establecimientoId: 'est-1', terminalId: '' })).toEqual({
      establecimientoId: 'est-1',
      terminalId: 'term-1'
    });
  });

  it('deja la terminal vacía cuando hay varias operables y ninguna elegida', () => {
    expect(resolverPuesto([central], { establecimientoId: 'est-1', terminalId: '' })).toBeNull();
  });
});
