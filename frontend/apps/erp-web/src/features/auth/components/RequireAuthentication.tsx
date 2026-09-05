import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuthSession } from '../model/useAuthSession';

export function RequireAuthentication() {
  const { authenticated } = useAuthSession();
  const location = useLocation();

  if (!authenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return <Outlet />;
}
