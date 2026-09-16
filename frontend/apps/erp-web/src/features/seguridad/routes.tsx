import type { RouteObject } from 'react-router';

export const securityRoutes = [
  {
    path: 'seguridad',
    lazy: async () => {
      const { SecurityPage } = await import('./pages/SecurityPage');
      return { Component: SecurityPage };
    }
  },
  {
    path: 'seguridad/permisos',
    lazy: async () => {
      const { PermissionsPage } = await import('./pages/PermissionsPage');
      return { Component: PermissionsPage };
    }
  }
] satisfies RouteObject[];
