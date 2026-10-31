import type { RouteObject } from 'react-router';

export const authRoutes = [
  {
    path: '/login',
    lazy: async () => {
      const { LoginPage } = await import('./pages/LoginPage');
      return { Component: LoginPage };
    }
  },
  {
    path: '/cambiar-contrasena',
    lazy: async () => {
      const { ChangePasswordPage } = await import('./pages/ChangePasswordPage');
      return { Component: ChangePasswordPage };
    }
  }
] satisfies RouteObject[];
