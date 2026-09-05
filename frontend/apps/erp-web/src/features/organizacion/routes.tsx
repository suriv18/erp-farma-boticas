import type { RouteObject } from 'react-router';

export const organizationRoutes = [
  {
    path: 'organizacion',
    lazy: async () => {
      const { OrganizationPage } = await import('./pages/OrganizationPage');
      return { Component: OrganizationPage };
    }
  }
] satisfies RouteObject[];
