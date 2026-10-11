import { expect, test } from '@playwright/test';
import { expectNoHorizontalOverflow } from './support/layout';
import { abrirVentasEn } from './support/ventas-api';

test.describe('Ventas', () => {
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
