import { useQuery } from '@tanstack/react-query';
import { perfilQuery } from '../api/profile.api';
import { branchLabelOf, initialsOf, roleLabelOf } from '../lib/profile-view';
import { useAuthSession } from './useAuthSession';

export type ProfileView = {
  name: string;
  initials: string;
  roleLabel: string | null;
  branchLabel: string | null;
};

const LOADING_NAME = 'Cargando…';
const FALLBACK_NAME = 'Mi cuenta';

export function useProfile(): ProfileView {
  const { userId } = useAuthSession();
  const { data, isLoading } = useQuery({
    ...perfilQuery(userId ?? ''),
    enabled: Boolean(userId)
  });

  if (!data) {
    const name = isLoading ? LOADING_NAME : FALLBACK_NAME;
    return { name, initials: initialsOf(name), roleLabel: null, branchLabel: null };
  }
  return {
    name: data.displayName,
    initials: initialsOf(data.displayName),
    roleLabel: roleLabelOf(data.assignments),
    branchLabel: branchLabelOf(data.assignments)
  };
}
