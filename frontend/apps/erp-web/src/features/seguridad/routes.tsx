import type { RouteObject } from 'react-router';

export const securityRoutes = [
  {
    path: 'seguridad',
    lazy: async () => {
      const { SecurityPage } = await import('./pages/SecurityPage');
      return { Component: SecurityPage };
    }
  }
] satisfies RouteObject[];
