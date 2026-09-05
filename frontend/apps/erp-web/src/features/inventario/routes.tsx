import type { RouteObject } from 'react-router';

export const inventoryRoutes = [
  {
    path: 'inventario',
    lazy: async () => {
      const { InventoryPage } = await import('./pages/InventoryPage');
      return { Component: InventoryPage };
    }
  }
] satisfies RouteObject[];
