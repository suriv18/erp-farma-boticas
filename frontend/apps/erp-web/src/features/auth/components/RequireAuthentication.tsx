import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuthSession } from '../model/useAuthSession';

export function RequireAuthentication() {
  const { status, passwordChangeRequired } = useAuthSession();
  const location = useLocation();

  if (status === 'loading') {
    return null;
  }

  if (status === 'unauthenticated') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (passwordChangeRequired) {
    return <Navigate to="/cambiar-contrasena" replace />;
  }

  return <Outlet />;
}
