export type PrincipioActivo = {
  id: string;
  codigoFuente: string | null;
  denominacion: string;
  nombreNormalizado: string | null;
  fuente: string | null;
  estado: string;
};

export type PrincipioActivoPayload = {
  codigoFuente?: string | undefined;
  denominacion: string;
  nombreNormalizado?: string | undefined;
  fuente?: string | undefined;
};
