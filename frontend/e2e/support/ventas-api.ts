import type { Page } from '@playwright/test';
import { sampleEstructura } from '../../apps/erp-web/src/test/inventario-fixtures';
import {
  sampleVenta,
  sampleVentaAnulada,
  sampleVentaResumen
} from '../../apps/erp-web/src/test/ventas-fixtures';
import { json, login } from './login';

const pagina = <T>(items: T[]) => ({ items, page: 0, size: 20, totalElements: items.length });

const ventaAnuladaResumen = {
  ...sampleVentaResumen,
  id: 'venta-2',
  numeroOperacion: 'EST001-T01-000002',
  estado: 'ANULADA'
};

export async function mockVentasApi(page: Page) {
  await page.route('**/api/v1/estructura-corporativa', (route) =>
    json(route, 200, sampleEstructura)
  );
  await page.route(/\/api\/v1\/ventas\/ventas(\/[^/?]+)?(\?|$)/, (route) => {
    const { pathname, searchParams } = new URL(route.request().url());
    const id = pathname.split('/').pop();
    if (id !== 'ventas') {
      return json(route, 200, id === 'venta-2' ? sampleVentaAnulada : sampleVenta);
    }
    const establecimientoId = searchParams.get('establecimientoId');
    return json(
      route,
      200,
      pagina(
        establecimientoId && establecimientoId !== 'est-1'
          ? []
          : [sampleVentaResumen, ventaAnuladaResumen]
      )
    );
  });
}

export async function abrirVentasEn(page: Page, path: string) {
  await mockVentasApi(page);
  await login(page);
  await page.goto(path);
}
