import type { RouteObject } from 'react-router';

export const catalogRoutes = [
  {
    path: 'catalogo',
    lazy: async () => {
      const { CatalogPage } = await import('./pages/CatalogPage');
      return { Component: CatalogPage };
    }
  }
] satisfies RouteObject[];
