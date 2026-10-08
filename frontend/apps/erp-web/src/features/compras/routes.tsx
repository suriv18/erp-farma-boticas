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
  },
  {
    path: 'compras/ordenes',
    lazy: async () => {
      const { OrdenesPage } = await import('./pages/OrdenesPage');
      return { Component: OrdenesPage };
    }
  },
  {
    path: 'compras/ordenes/nueva',
    lazy: async () => {
      const { NuevaOrdenPage } = await import('./pages/NuevaOrdenPage');
      return { Component: NuevaOrdenPage };
    }
  },
  {
    path: 'compras/ordenes/:ordenId',
    lazy: async () => {
      const { OrdenDetailPage } = await import('./pages/OrdenDetailPage');
      return { Component: OrdenDetailPage };
    }
  },
  {
    path: 'compras/ordenes/:ordenId/recepcion',
    lazy: async () => {
      const { RecepcionPage } = await import('./pages/RecepcionPage');
      return { Component: RecepcionPage };
    }
  }
] satisfies RouteObject[];
