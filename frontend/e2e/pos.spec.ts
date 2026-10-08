import { expect, test } from '@playwright/test';
import { expectNoHorizontalOverflow } from './support/layout';
import { abrirPosEn } from './support/pos-api';

test.describe('Punto de venta', () => {
  test('vende en efectivo con vuelto y muestra el comprobante', async ({ page }) => {
    await abrirPosEn(page, '/pos');

    await expect(page.getByRole('heading', { name: 'Punto de venta', exact: true })).toBeVisible();
    await page.getByLabel('Buscar producto').fill('paracetamol');
    await page.getByRole('button', { name: 'Buscar' }).click();
    await page.getByRole('button', { name: 'Agregar MED-001' }).click();
    await page.getByLabel('Cantidad de MED-001').fill('2');
    await page.getByLabel('Monto recibido').fill('30');
    await expect(page.getByText('S/ 5.00').first()).toBeVisible();
    await expectNoHorizontalOverflow(page);

    await page.getByRole('button', { name: 'Cobrar' }).click();

    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.getByText('Venta registrada')).toBeVisible();
    await expect(
      page.getByText('Comprobante interno — no válido como comprobante de pago')
    ).toBeVisible();
    await page.getByRole('button', { name: 'Nueva venta' }).click();
    await expect(page.getByText('El carrito está vacío.')).toBeVisible();
  });
});
