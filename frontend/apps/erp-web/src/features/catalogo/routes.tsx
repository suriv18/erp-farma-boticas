import type { RouteObject } from 'react-router';

export const catalogRoutes = [
  {
    path: 'catalogo',
    lazy: async () => {
      const { CatalogPage } = await import('./pages/CatalogPage');
      return { Component: CatalogPage };
    }
  },
  {
    path: 'catalogo/marcas',
    lazy: async () => {
      const { MarcasPage } = await import('./pages/MarcasPage');
      return { Component: MarcasPage };
    }
  },
  {
    path: 'catalogo/categorias',
    lazy: async () => {
      const { CategoriasPage } = await import('./pages/CategoriasPage');
      return { Component: CategoriasPage };
    }
  }
] satisfies RouteObject[];
