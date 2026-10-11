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
      const { ClasificacionesControladasPage } =
        await import('./pages/ClasificacionesControladasPage');
      return { Component: ClasificacionesControladasPage };
    }
  },
  {
    path: 'catalogo/tipos-documento-identidad',
    lazy: async () => {
      const { TiposDocumentoIdentidadPage } = await import('./pages/TiposDocumentoIdentidadPage');
      return { Component: TiposDocumentoIdentidadPage };
    }
  },
  {
    path: 'catalogo/principios-activos',
    lazy: async () => {
      const { PrincipiosActivosPage } = await import('./pages/PrincipiosActivosPage');
      return { Component: PrincipiosActivosPage };
    }
  },
  {
    path: 'catalogo/productos-regulados',
    lazy: async () => {
      const { ProductosReguladosPage } = await import('./pages/ProductosReguladosPage');
      return { Component: ProductosReguladosPage };
    }
  },
  {
    path: 'catalogo/productos-regulados/:productoReguladoId',
    lazy: async () => {
      const { ProductoReguladoDetailPage } = await import('./pages/ProductoReguladoDetailPage');
      return { Component: ProductoReguladoDetailPage };
    }
  },
  {
    path: 'catalogo/skus',
    lazy: async () => {
      const { SkusPage } = await import('./pages/SkusPage');
      return { Component: SkusPage };
    }
  },
  {
    path: 'catalogo/skus/:skuId',
    lazy: async () => {
      const { SkuDetailPage } = await import('./pages/SkuDetailPage');
      return { Component: SkuDetailPage };
    }
  }
] satisfies RouteObject[];
