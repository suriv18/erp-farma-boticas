import type { RouteObject } from 'react-router';

export const posRoutes = [
  {
    path: 'pos',
    lazy: async () => {
      const { PosPage } = await import('./pages/PosPage');
      return { Component: PosPage };
    }
  }
] satisfies RouteObject[];
