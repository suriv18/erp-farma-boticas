import { motivoSinAltasEmpresa, motivoSinAltasEstablecimiento } from './altas';

describe('motivoSinAltasEmpresa', () => {
  it('no hay motivo cuando la empresa está activa', () => {
    expect(motivoSinAltasEmpresa('ACTIVO')).toBeNull();
  });

  it.each([
    ['SUSPENDIDO', 'La empresa está suspendida; no admite establecimientos nuevos.'],
    ['BLOQUEADO', 'La empresa está bloqueada; no admite establecimientos nuevos.']
  ] as const)('explica el bloqueo con la empresa %s', (estado, mensaje) => {
    expect(motivoSinAltasEmpresa(estado)).toBe(mensaje);
  });
});

describe('motivoSinAltasEstablecimiento', () => {
  it.each(['ACTIVO', 'REMODELACION'] as const)(
    'no hay motivo con el establecimiento %s',
    (estado) => {
      expect(motivoSinAltasEstablecimiento(estado)).toBeNull();
    }
  );

  it.each([
    [
      'SUSPENDIDO',
      'El establecimiento está suspendido; no admite almacenes ni terminales POS nuevos.'
    ],
    [
      'CLAUSURADO',
      'El establecimiento está clausurado; no admite almacenes ni terminales POS nuevos.'
    ]
  ] as const)('explica el bloqueo con el establecimiento %s', (estado, mensaje) => {
    expect(motivoSinAltasEstablecimiento(estado)).toBe(mensaje);
  });
});
