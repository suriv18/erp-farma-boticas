import { createBrowserRouter, Navigate } from 'react-router';
import { RequireAuthentication } from '../features/auth';
import { NotFoundPage } from '../shared/pages/NotFoundPage';
import { AppShell } from '../shared/layout/AppShell';
import { erpFeatureRoutes, publicFeatureRoutes } from './feature-routes';

export const router = createBrowserRouter([
  ...publicFeatureRoutes,
  {
    element: <RequireAuthentication />,
    children: [
      {
        path: '/',
        element: <AppShell />,
        children: [
          { index: true, element: <Navigate to="/dashboard" replace /> },
          ...erpFeatureRoutes,
          { path: '*', element: <NotFoundPage /> }
        ]
      }
    ]
  }
]);
