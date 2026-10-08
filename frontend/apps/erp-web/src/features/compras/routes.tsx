import type { RouteObject } from 'react-router';

export const purchasesRoutes = [
  {
    path: 'compras',
    lazy: async () => {
      const { PurchasesPage } = await import('./pages/PurchasesPage');
      return { Component: PurchasesPage };
    }
  },
  {
    path: 'compras/proveedores',
    lazy: async () => {
      const { ProveedoresPage } = await import('./pages/ProveedoresPage');
      return { Component: ProveedoresPage };
    }
  },
  {
    path: 'compras/proveedores/nuevo',
    lazy: async () => {
      const { NuevoProveedorPage } = await import('./pages/NuevoProveedorPage');
      return { Component: NuevoProveedorPage };
    }
  },
  {
    path: 'compras/proveedores/:proveedorId',
    lazy: async () => {
      const { ProveedorDetailPage } = await import('./pages/ProveedorDetailPage');
      return { Component: ProveedorDetailPage };
    }
  }
] satisfies RouteObject[];
