import type { RouteObject } from 'react-router';
import { authRoutes } from '../features/auth';
import { cashRegisterRoutes } from '../features/caja';
import { catalogRoutes } from '../features/catalogo';
import { customerRoutes } from '../features/clientes';
import { purchasesRoutes } from '../features/compras';
import { dashboardRoutes } from '../features/dashboard';
import { inventoryRoutes } from '../features/inventario';
import { organizationRoutes } from '../features/organizacion';
import { posRoutes } from '../features/pos';
import { securityRoutes } from '../features/seguridad';
import { salesRoutes } from '../features/ventas';

export const publicFeatureRoutes = [...authRoutes] satisfies RouteObject[];

export const erpFeatureRoutes = [
  ...dashboardRoutes,
  ...catalogRoutes,
  ...inventoryRoutes,
  ...purchasesRoutes,
  ...salesRoutes,
  ...posRoutes,
  ...cashRegisterRoutes,
  ...customerRoutes,
  ...securityRoutes,
  ...organizationRoutes
] satisfies RouteObject[];
