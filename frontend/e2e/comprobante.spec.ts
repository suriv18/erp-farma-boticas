import { expect, test } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { abrirVentasEn, mockVentasApi } from './support/ventas-api';
import { expectNoHorizontalOverflow } from './support/layout';
import { sampleVenta } from '../apps/erp-web/src/test/ventas-fixtures';
import { json, login } from './support/login';

test('ticket de 80 mm legible en móvil y ambos temas, con impresión aislada', async ({
  page
}, testInfo) => {
  await abrirVentasEn(page, '/ventas/venta-1');
  await page.getByRole('button', { name: 'Ver comprobante', exact: true }).click();
  const ticket = page.getByRole('article', { name: 'Comprobante interno de la venta' });
  await expect(ticket).toBeVisible();
  for (const theme of ['light', 'dark']) {
    await page.evaluate((value) => {
      document.documentElement.dataset.theme = value;
    }, theme);
    await expect(ticket).toHaveCSS('background-color', 'rgb(255, 255, 255)');
    const result = await new AxeBuilder({ page })
      .include('.comprobante-imprimible')
      .withTags(['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa'])
      .analyze();
    expect(result.violations).toEqual([]);
  }
  await ticket.screenshot({ path: testInfo.outputPath('ticket-80mm.png') });
  await page.setViewportSize({ width: 320, height: 900 });
  await expectNoHorizontalOverflow(page);
  await page.getByRole('button', { name: 'Ocultar comprobante' }).click();
  await page.emulateMedia({ media: 'print' });
  await expect(ticket).toBeVisible();
  expect((await ticket.boundingBox())!.width).toBeCloseTo((80 * 96) / 25.4, 0);
  await expect(page.getByRole('heading', { name: 'Importes de la venta' })).toBeHidden();
  const bounds = await ticket.boundingBox();
  expect(bounds!.x).toBe(0);
  expect(bounds!.y).toBe(0);
  expect(
    await page.locator('body').evaluate((el) => el.getBoundingClientRect().height)
  ).toBeLessThanOrEqual(bounds!.height + 1);
});

test('imprime todos los productos de un ticket largo sin truncar el pie', async ({
  page
}, testInfo) => {
  await mockVentasApi(page);
  const lineas = Array.from({ length: 30 }, (_, index) => ({
    ...sampleVenta.lineas[0]!,
    numeroLinea: index + 1,
    descripcion: `Producto ${index + 1}: presentación comercial con una descripción extensa para verificar su lectura completa`
  }));
  await page.route('**/api/v1/ventas/ventas/venta-1', (route) =>
    json(route, 200, {
      ...sampleVenta,
      lineas,
      subtotal: 635.7,
      impuestoTotal: 114.3,
      total: 750,
      pago: { ...sampleVenta.pago, monto: 750, montoRecibido: 800, vuelto: 50 }
    })
  );
  await login(page);
  await page.goto('/ventas/venta-1');
  await expect(page.getByRole('button', { name: 'Ver comprobante', exact: true })).toBeVisible();
  await page.emulateMedia({ media: 'print' });
  const ticket = page.locator('.comprobante-imprimible');
  await expect(ticket.locator('li')).toHaveCount(30);
  await expect(ticket.getByText('Gracias por su compra')).toBeVisible();
  const bounds = await ticket.boundingBox();
  const footer = await ticket.locator('footer').boundingBox();
  expect(footer!.y + footer!.height).toBeLessThanOrEqual(bounds!.y + bounds!.height);
  expect(await ticket.evaluate((el) => el.scrollWidth <= el.clientWidth)).toBe(true);
  await ticket.screenshot({ path: testInfo.outputPath('ticket-largo.png') });
});
