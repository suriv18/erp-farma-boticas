import { expect, test } from '@playwright/test';
import { expectNoHorizontalOverflow } from './support/layout';
import { abrirPosEn } from './support/pos-api';
import AxeBuilder from '@axe-core/playwright';

test.describe('Punto de venta', () => {
  test('carrito adaptable y cobro accesible en ambos temas', async ({ page }, testInfo) => {
    await abrirPosEn(page, '/pos');
    await page.getByLabel('Buscar producto', { exact: true }).fill('paracetamol');
    await page.getByRole('button', { name: 'Buscar', exact: true }).click();
    await page.getByRole('button', { name: 'Agregar MED-001' }).click();
    await expect(page.getByRole('button', { name: /^Cobrar / })).toBeDisabled();
    await expect(page.getByText('Ingresa un monto válido igual o mayor al total.')).toBeVisible();
    for (const theme of ['light', 'dark']) {
      const toggle = page.getByRole('button', { name: theme === 'dark' ? 'Cambiar a modo oscuro' : 'Cambiar a modo claro' });
      if (await toggle.count()) await toggle.click();
      await expect(page.locator('html')).toHaveAttribute('data-theme', theme);
      await page.evaluate(async () => {
        await Promise.all(document.getAnimations().map((animation) => animation.finished.catch(() => {})));
      });
      expect((await new AxeBuilder({ page }).include('main').withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze()).violations).toEqual([]);
    }
    await page.screenshot({ path: testInfo.outputPath('pos.png'), fullPage: true });
    await page.setViewportSize({ width: 320, height: 800 });
    await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)).toBeLessThanOrEqual(1);
    await expectNoHorizontalOverflow(page);
    await page.getByRole('button', { name: 'Ir al cobro' }).click();
    await expect(page.getByLabel('Monto recibido')).toBeFocused();
    await page.getByLabel('Monto recibido').fill('20');
    await expect(page.getByRole('button', { name: /^Cobrar / })).toBeEnabled();
    await page.getByRole('button', { name: 'Quitar MED-001' }).click();
    await expect(page.getByRole('button', { name: 'Ir al cobro' })).toBeHidden();
    await expect(page.getByRole('button', { name: /^Cobrar / })).toBeDisabled();
  });
  test('vende en efectivo con vuelto y muestra el comprobante', async ({ page }) => {
    await abrirPosEn(page, '/pos');

    await expect(page.getByRole('heading', { name: 'Punto de venta', exact: true })).toBeVisible();
    await page.getByLabel('Buscar producto', { exact: true }).fill('paracetamol');
    await page.getByRole('button', { name: 'Buscar' }).click();
    await page.getByRole('button', { name: 'Agregar MED-001' }).click();
    await page.getByLabel('Cantidad de MED-001', { exact: true }).fill('2');
    await expect(page.getByLabel('Cantidad de MED-001', { exact: true })).toHaveCSS('width', '70px');
    await expect(page.getByLabel('Precio de MED-001')).toHaveCSS('width', '95px');
    await page.getByLabel('Monto recibido').fill('30');
    await expect(page.getByText('S/ 5.00').first()).toBeVisible();
    await expectNoHorizontalOverflow(page);

    await page.getByRole('button', { name: 'Cobrar' }).click();

    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.getByText('Venta registrada')).toBeVisible();
    await expect(
      page.getByText('Comprobante interno — no válido como comprobante de pago')
    ).toBeVisible();
    await page.emulateMedia({ media: 'print' });
    const ticket = page.locator('.comprobante-imprimible');
    await expect(ticket).toBeVisible();
    await expect(page.getByRole('button', { name: 'Nueva venta' })).toBeHidden();
    expect((await ticket.boundingBox())!.width).toBeCloseTo((80 * 96) / 25.4, 0);
    await expect(page.getByRole('dialog')).toHaveCSS('transform', 'none');
    await expect(page.getByRole('dialog')).toHaveCSS('max-height', 'none');
    await page.emulateMedia({ media: 'screen' });
    await page.getByRole('button', { name: 'Nueva venta' }).click();
    await expect(page.getByText('El carrito está vacío.')).toBeVisible();
  });
});
