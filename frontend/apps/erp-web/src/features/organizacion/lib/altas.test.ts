import { motivoSinAltasEmpresa, motivoSinAltasEstablecimiento } from './altas';

describe('motivoSinAltasEmpresa', () => {
  it('no hay motivo cuando la empresa está activa', () => {
    expect(motivoSinAltasEmpresa('ACTIVO')).toBeNull();
  });

  it.each(['SUSPENDIDO', 'BLOQUEADO'] as const)(
    'explica el bloqueo con la empresa %s',
    (estado) => {
      expect(motivoSinAltasEmpresa(estado)).toBe(
        `La empresa está ${estado}; no admite establecimientos nuevos.`
      );
    }
  );
});

describe('motivoSinAltasEstablecimiento', () => {
  it.each(['ACTIVO', 'REMODELACION'] as const)(
    'no hay motivo con el establecimiento %s',
    (estado) => {
      expect(motivoSinAltasEstablecimiento(estado)).toBeNull();
    }
  );

  it.each(['SUSPENDIDO', 'CLAUSURADO'] as const)(
    'explica el bloqueo con el establecimiento %s',
    (estado) => {
      expect(motivoSinAltasEstablecimiento(estado)).toBe(
        `El establecimiento está ${estado}; no admite almacenes ni terminales POS nuevos.`
      );
    }
  );
});
