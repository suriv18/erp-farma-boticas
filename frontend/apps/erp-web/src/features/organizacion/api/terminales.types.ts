export const ESTADOS_TERMINAL = ['ACTIVO', 'BLOQUEADO', 'MANTENIMIENTO'] as const;

export type EstadoTerminal = (typeof ESTADOS_TERMINAL)[number];

export type Terminal = {
  id: string;
  tenantId: string;
  establecimientoId: string;
  codigo: string;
  nombre: string;
  serieBoletaDefecto: string | null;
  serieFacturaDefecto: string | null;
  numeroSerieEquipo: string | null;
  hostname: string | null;
  ipEquipo: string | null;
  impresoraCodigo: string | null;
  storeEdgeHabilitado: boolean;
  estado: EstadoTerminal;
  createdAt: string;
  updatedAt: string | null;
};

type TerminalDatos = {
  nombre: string;
  serieBoletaDefecto?: string | undefined;
  serieFacturaDefecto?: string | undefined;
  numeroSerieEquipo?: string | undefined;
  hostname?: string | undefined;
  ipEquipo?: string | undefined;
  impresoraCodigo?: string | undefined;
  storeEdgeHabilitado: boolean;
};

export type CrearTerminalPayload = TerminalDatos & {
  tenantId: string;
  establecimientoId: string;
  codigo: string;
};

export type ActualizarTerminalPayload = TerminalDatos & {
  estado: EstadoTerminal;
};
