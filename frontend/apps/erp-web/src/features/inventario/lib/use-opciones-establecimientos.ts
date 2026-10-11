import { useQuery } from '@tanstack/react-query';
import { corporateStructureQuery } from '../../organizacion';
import { opcionesEstablecimientos, type OpcionEstablecimiento } from './estructura';

export function useOpcionesEstablecimientos(): OpcionEstablecimiento[] {
  const { data } = useQuery(corporateStructureQuery);
  return opcionesEstablecimientos(data);
}
