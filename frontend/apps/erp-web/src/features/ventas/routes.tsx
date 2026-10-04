import type { RouteObject } from 'react-router';

export const salesRoutes = [
  {
    path: 'ventas',
    lazy: async () => {
      const { SalesPage } = await import('./pages/SalesPage');
      return { Component: SalesPage };
    }
  },
  {
    path: 'ventas/:ventaId',
    lazy: async () => {
      const { SaleDetailPage } = await import('./pages/SaleDetailPage');
      return { Component: SaleDetailPage };
    }
  }
] satisfies RouteObject[];
