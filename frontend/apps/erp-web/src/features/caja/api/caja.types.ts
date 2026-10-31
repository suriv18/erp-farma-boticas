export type EstadoTurno = 'ABIERTO' | 'EN_ARQUEO' | 'CERRADO' | 'ANULADO';

export type Turno = {
  id: string;
  terminalId: string;
  establecimientoId: string;
  cajeroId: string;
  aperturaAt: string;
  fondoInicial: number;
  estado: EstadoTurno;
  cierreAt: string | null;
  totalVentasSistema: number | null;
  totalSistema: number | null;
  totalDeclarado: number | null;
  diferencia: number | null;
  observacionCierre: string | null;
};

export type AbrirTurnoPayload = {
  terminalId: string;
  fondoInicial: number;
};

export type CerrarTurnoPayload = {
  totalDeclarado: number;
  observacion?: string | undefined;
};
