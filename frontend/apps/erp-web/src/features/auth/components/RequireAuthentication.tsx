import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuthSession } from '../model/useAuthSession';

export function RequireAuthentication() {
  const { status } = useAuthSession();
  const location = useLocation();

  if (status === 'loading') {
    return null;
  }

  if (status === 'unauthenticated') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return <Outlet />;
}
