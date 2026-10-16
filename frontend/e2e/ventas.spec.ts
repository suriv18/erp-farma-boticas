import { expect, test } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { expectNoHorizontalOverflow } from './support/layout';
import { abrirVentasEn } from './support/ventas-api';

test.describe('Ventas', () => {
  test('organiza el detalle sin desbordamientos y permite previsualizar e imprimir', async ({
    page
  }, testInfo) => {
    await abrirVentasEn(page, '/ventas/venta-1');
    await expect(page.getByRole('heading', { name: 'Importes de la venta' })).toBeVisible();
    await expect(page.getByRole('region', { name: 'Vista previa del comprobante' })).toBeHidden();
    await page.screenshot({ path: testInfo.outputPath('venta-detalle.png'), fullPage: true });
    await page.getByRole('button', { name: 'Ver comprobante', exact: true }).click();
    await expect(page.getByRole('region', { name: 'Vista previa del comprobante' })).toBeVisible();
    await expectNoHorizontalOverflow(page);
    await page.getByRole('button', { name: 'Ocultar comprobante' }).click();
    await page.emulateMedia({ media: 'print' });
    await expect(page.locator('.comprobante-imprimible')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Imprimir comprobante' })).toBeHidden();
    await page.emulateMedia({ media: 'screen' });
    for (const width of [1440, 768, 320]) {
      await page.setViewportSize({ width, height: 900 });
      await expectNoHorizontalOverflow(page);
    }
  });

  test('detalle y anulación accesibles en claro y oscuro', async ({ page }) => {
    await abrirVentasEn(page, '/ventas/venta-2');
    await expect(page.getByRole('heading', { name: 'Anulación', exact: true })).toBeVisible();
    for (const theme of ['light', 'dark']) {
      await page.evaluate((value) => {
        document.documentElement.dataset.theme = value;
      }, theme);
      await page.evaluate(async () => {
        await Promise.all(
          document.getAnimations().map((animation) => animation.finished.catch(() => {}))
        );
      });
      const results = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa'])
        .analyze();
      expect(results.violations).toEqual([]);
    }
  });
  test('lista el historial y abre el detalle con el comprobante', async ({ page }) => {
    await abrirVentasEn(page, '/ventas');

    await expect(page.getByRole('heading', { name: 'Ventas', exact: true })).toBeVisible();
    await expect(
      page.getByRole('link', { name: 'Ver detalle de la venta EST001-T01-000001' })
    ).toBeVisible();
    await expectNoHorizontalOverflow(page);

    await page.getByRole('link', { name: 'Ver detalle de la venta EST001-T01-000001' }).click();

    await expect(page.getByRole('heading', { name: 'Venta EST001-T01-000001' })).toBeVisible();
    await expect(page.getByText('Paracetamol 500 mg').first()).toBeVisible();
    await expect(page.getByRole('button', { name: 'Imprimir comprobante' })).toBeVisible();
    await expectNoHorizontalOverflow(page);
  });

  test('muestra el motivo de una venta anulada', async ({ page }) => {
    await abrirVentasEn(page, '/ventas/venta-2');

    await expect(page.getByText('Error de digitación').first()).toBeVisible();
  });
});
