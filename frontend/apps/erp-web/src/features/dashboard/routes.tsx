import type { RouteObject } from 'react-router';

export const dashboardRoutes = [
  {
    path: 'dashboard',
    lazy: async () => {
      const { DashboardPage } = await import('./pages/DashboardPage');
      return { Component: DashboardPage };
    }
  }
] satisfies RouteObject[];
