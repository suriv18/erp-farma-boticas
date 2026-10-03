import type { RouteObject } from 'react-router';

export const inventoryRoutes = [
  {
    path: 'inventario',
    lazy: async () => {
      const { InventoryPage } = await import('./pages/InventoryPage');
      return { Component: InventoryPage };
    }
  },
  {
    path: 'inventario/lotes/:loteId',
    lazy: async () => {
      const { LoteDetailPage } = await import('./pages/LoteDetailPage');
      return { Component: LoteDetailPage };
    }
  }
] satisfies RouteObject[];
