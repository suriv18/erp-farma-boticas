import { useQuery } from '@tanstack/react-query';
import { miPuestoQuery } from '../api/mi-puesto.api';
import type { MiPuestoEstablecimiento } from '../api/mi-puesto.types';

type MiPuestoEstado = {
  establecimientos: MiPuestoEstablecimiento[];
  isPending: boolean;
  isError: boolean;
};

export function useMiPuesto(): MiPuestoEstado {
  const { data, isPending, isError } = useQuery(miPuestoQuery);
  return { establecimientos: data?.establecimientos ?? [], isPending, isError };
}
