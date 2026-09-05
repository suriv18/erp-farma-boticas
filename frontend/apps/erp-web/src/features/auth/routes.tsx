import type { RouteObject } from 'react-router';

export const authRoutes = [
  {
    path: '/login',
    lazy: async () => {
      const { LoginPage } = await import('./pages/LoginPage');
      return { Component: LoginPage };
    }
  }
] satisfies RouteObject[];
