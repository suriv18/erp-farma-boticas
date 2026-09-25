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
  },
  {
    path: 'catalogo/rubros-comerciales',
    lazy: async () => {
      const { RubrosComercialesPage } = await import('./pages/RubrosComercialesPage');
      return { Component: RubrosComercialesPage };
    }
  },
  {
    path: 'catalogo/condiciones-venta',
    lazy: async () => {
      const { CondicionesVentaPage } = await import('./pages/CondicionesVentaPage');
      return { Component: CondicionesVentaPage };
    }
  },
  {
    path: 'catalogo/formas-farmaceuticas',
    lazy: async () => {
      const { FormasFarmaceuticasPage } = await import('./pages/FormasFarmaceuticasPage');
      return { Component: FormasFarmaceuticasPage };
    }
  },
  {
    path: 'catalogo/vias-administracion',
    lazy: async () => {
      const { ViasAdministracionPage } = await import('./pages/ViasAdministracionPage');
      return { Component: ViasAdministracionPage };
    }
  },
  {
    path: 'catalogo/unidades-medida',
    lazy: async () => {
      const { UnidadesMedidaPage } = await import('./pages/UnidadesMedidaPage');
      return { Component: UnidadesMedidaPage };
    }
  },
  {
    path: 'catalogo/clasificaciones-controladas',
    lazy: async () => {
      const { ClasificacionesControladasPage } = await import('./pages/ClasificacionesControladasPage');
      return { Component: ClasificacionesControladasPage };
    }
  }
] satisfies RouteObject[];
