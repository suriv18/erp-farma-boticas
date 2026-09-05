import type { RouteObject } from 'react-router';

export const purchasesRoutes = [
  {
    path: 'compras',
    lazy: async () => {
      const { PurchasesPage } = await import('./pages/PurchasesPage');
      return { Component: PurchasesPage };
    }
  }
] satisfies RouteObject[];
