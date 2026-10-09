import { useQuery } from '@tanstack/react-query';
import { corporateStructureQuery, type EstablishmentStructure } from '../api/organization.api';

type EstablecimientosEstado = {
  establecimientos: EstablishmentStructure[];
  isPending: boolean;
  isError: boolean;
};

export function useEstablecimientosEstado(): EstablecimientosEstado {
  const { data, isPending, isError } = useQuery(corporateStructureQuery);
  return {
    establecimientos: (data?.companies ?? []).flatMap(({ establishments }) => establishments),
    isPending,
    isError
  };
}

export const useEstablecimientos = (): EstablishmentStructure[] =>
  useEstablecimientosEstado().establecimientos;
