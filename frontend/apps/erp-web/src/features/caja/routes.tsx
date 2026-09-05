import type { RouteObject } from 'react-router';

export const cashRegisterRoutes = [
  {
    path: 'caja',
    lazy: async () => {
      const { CashRegisterPage } = await import('./pages/CashRegisterPage');
      return { Component: CashRegisterPage };
    }
  }
] satisfies RouteObject[];
