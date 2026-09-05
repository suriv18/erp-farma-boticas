import type { RouteObject } from 'react-router';

export const customerRoutes = [
  {
    path: 'clientes',
    lazy: async () => {
      const { CustomersPage } = await import('./pages/CustomersPage');
      return { Component: CustomersPage };
    }
  }
] satisfies RouteObject[];
