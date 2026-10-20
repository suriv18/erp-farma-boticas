import { tienePermiso } from '../lib/permisos';
import { useProfile } from './useProfile';

export function usePermisos() {
  const { permissions } = useProfile();
  return { puede: (requerido: string) => tienePermiso(permissions, requerido) };
}
