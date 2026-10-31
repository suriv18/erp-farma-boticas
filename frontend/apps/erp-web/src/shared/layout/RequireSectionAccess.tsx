import { Outlet, useLocation } from 'react-router';
import { useProfile } from '../../features/auth';
import { ForbiddenPage } from '../pages/ForbiddenPage';
import { puedeAcceder } from './acceso-secciones';

export function RequireSectionAccess() {
  const { pathname } = useLocation();
  const { permissions, permissionsReady } = useProfile();

  if (!permissionsReady) {
    return null;
  }

  return puedeAcceder(permissions, pathname) ? <Outlet /> : <ForbiddenPage />;
}
