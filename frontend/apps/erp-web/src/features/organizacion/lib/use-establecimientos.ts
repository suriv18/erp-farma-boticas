import { useQuery } from '@tanstack/react-query';
import { corporateStructureQuery, type EstablishmentStructure } from '../api/organization.api';

export function useEstablecimientos(): EstablishmentStructure[] {
  const { data } = useQuery(corporateStructureQuery);
  return (data?.companies ?? []).flatMap(({ establishments }) => establishments);
}
