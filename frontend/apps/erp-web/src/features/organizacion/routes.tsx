import type { RouteObject } from 'react-router';

export const organizationRoutes = [
  {
    path: 'organizacion',
    lazy: async () => {
      const { OrganizationPage } = await import('./pages/OrganizationPage');
      return { Component: OrganizationPage };
    }
  },
  {
    path: 'organizacion/empresas',
    lazy: async () => {
      const { EmpresasPage } = await import('./pages/EmpresasPage');
      return { Component: EmpresasPage };
    }
  },
  {
    path: 'organizacion/empresas/:empresaId',
    lazy: async () => {
      const { EmpresaDetailPage } = await import('./pages/EmpresaDetailPage');
      return { Component: EmpresaDetailPage };
    }
  },
  {
    path: 'organizacion/establecimientos/:establecimientoId',
    lazy: async () => {
      const { EstablecimientoDetailPage } = await import('./pages/EstablecimientoDetailPage');
      return { Component: EstablecimientoDetailPage };
    }
  }
] satisfies RouteObject[];
