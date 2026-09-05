import type { RouteObject } from 'react-router';

export const salesRoutes = [
  {
    path: 'ventas',
    lazy: async () => {
      const { SalesPage } = await import('./pages/SalesPage');
      return { Component: SalesPage };
    }
  }
] satisfies RouteObject[];
