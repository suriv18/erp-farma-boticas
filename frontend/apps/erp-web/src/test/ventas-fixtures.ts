import type { Turno } from '../features/caja/api/caja.types';

export const sampleTurno: Turno = {
  id: 'turno-1',
  terminalId: 'term-1',
  establecimientoId: 'est-1',
  cajeroId: 'user-1',
  aperturaAt: '2026-10-03T13:00:00Z',
  fondoInicial: 100,
  estado: 'ABIERTO',
  cierreAt: null,
  totalVentasSistema: null,
  totalSistema: null,
  totalDeclarado: null,
  diferencia: null,
  observacionCierre: null
};

export const sampleTurnoCerrado: Turno = {
  ...sampleTurno,
  estado: 'CERRADO',
  cierreAt: '2026-10-03T21:00:00Z',
  totalVentasSistema: 250,
  totalSistema: 350,
  totalDeclarado: 348.5,
  diferencia: -1.5,
  observacionCierre: 'Faltante de monedas'
};
