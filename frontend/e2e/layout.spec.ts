import { expect, test } from '@playwright/test';
import { json, login } from './support/login';
import { mockInventarioApi } from './support/inventario-api';
import { mockComprasApi } from './support/compras-api';
import { mockVentasApi } from './support/ventas-api';
import { mockPosApi } from './support/pos-api';
import { mockOrganizacionApi } from './support/organizacion-api';
import { expectNoHorizontalOverflow } from './support/layout';

const routes = [
  '/dashboard',
  '/catalogo',
  '/inventario',
  '/compras',
  '/compras/proveedores',
  '/ventas',
  '/pos',
  '/caja',
  '/clientes',
  '/seguridad',
  '/organizacion',
  '/catalogo/skus/nuevo',
  '/catalogo/productos-regulados/nuevo',
  '/compras/proveedores/nuevo',
  '/compras/ordenes/nueva'
];

for (const width of [1920, 1440, 1024, 768, 390, 320]) {
  test(`aplica el layout compartido en todos los módulos a ${width}px`, async ({
    page
  }, testInfo) => {
    test.setTimeout(120_000);
    await page.setViewportSize({ width, height: 900 });
    await mockInventarioApi(page);
    await mockComprasApi(page);
    await mockPosApi(page);
    await mockVentasApi(page);
    await mockOrganizacionApi(page);
    await page.route('**/api/v1/catalogo/**', (route) => {
      const path = new URL(route.request().url()).pathname;
      return json(
        route,
        200,
        ['marcas', 'categorias', 'productos-regulados'].some((name) => path.endsWith('/' + name))
          ? { items: [], page: 0, size: 100, totalElements: 0 }
          : []
      );
    });
    await login(page);
    for (const route of routes) {
      await test.step(route, async () => {
        await page.goto(route);
        const content = page.locator('main > .erp-page');
        await expect(content).toBeVisible();
        const padding = width >= 640 ? 24 : 16;
        const offset = width >= 1024 ? 224 : 0;
        const bounds = await content.boundingBox();
        expect(bounds?.x).toBe(offset + padding);
        expect(bounds?.width).toBe(width - offset - padding * 2);
        expect(width - (bounds!.x + bounds!.width)).toBe(padding);
        await expectNoHorizontalOverflow(page);
        if (route === '/catalogo/skus/nuevo' && [1920, 390].includes(width)) {
          await page.screenshot({
            path: testInfo.outputPath(`layout-sku-${width}.png`),
            fullPage: true
          });
        }
      });
    }
    if (width >= 1024) {
      expect((await page.locator('.erp-sidebar').boundingBox())?.width).toBe(224);
    } else {
      await expect(page.locator('.erp-sidebar')).toBeHidden();
      await page.getByRole('button', { name: 'Abrir menú' }).click();
      await expect(page.getByRole('navigation', { name: 'Navegación principal' })).toBeVisible();
      await expectNoHorizontalOverflow(page);
    }
  });
}
