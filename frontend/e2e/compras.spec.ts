import { expect, test } from '@playwright/test';
import { abrirComprasEn } from './support/compras-api';
import { expectNoHorizontalOverflow } from './support/layout';

test.describe('Compras', () => {
  test('crea un proveedor, lo edita y cambia su estado', async ({ page }) => {
    await abrirComprasEn(page, '/compras');

    await expect(page.getByRole('heading', { name: 'Compras', exact: true })).toBeVisible();
    await page.getByRole('link', { name: /Proveedores/ }).click();
    await expect(page.getByText('Laboratorios Perú SAC')).toBeVisible();
    await expectNoHorizontalOverflow(page);

    await page.getByRole('link', { name: 'Nuevo proveedor' }).click();
    await page.getByLabel('Número de documento').fill('20512345678');
    await page.getByLabel('Razón social').fill('Droguería Andina SAC');
    await page.getByRole('button', { name: 'Crear proveedor' }).click();

    await expect(page.getByRole('heading', { name: 'Droguería Andina SAC' })).toBeVisible();
    await page.getByLabel('Nombre comercial').fill('Andina');
    await page.getByRole('button', { name: 'Guardar cambios' }).click();
    await expect(page.getByText('Proveedor actualizado.')).toBeVisible();

    await page.getByRole('button', { name: 'Cambiar estado' }).click();
    await page.getByLabel('Estado', { exact: true }).selectOption('SUSPENDIDO');
    await page.getByRole('button', { name: 'Guardar estado' }).click();
    await expect(page.getByText('SUSPENDIDO')).toBeVisible();
    await expectNoHorizontalOverflow(page);
  });

  test('aprueba, emite y anula una orden de compra', async ({ page }) => {
    await abrirComprasEn(page, '/compras');

    await page.getByRole('link', { name: /Órdenes de compra/ }).click();
    await expect(page.getByText('OC-2026-000001')).toBeVisible();
    await expectNoHorizontalOverflow(page);
    await page.getByRole('link', { name: 'Ver detalle de la orden OC-2026-000001' }).click();

    await expect(page.getByRole('heading', { name: 'Orden OC-2026-000001' })).toBeVisible();
    await page.getByRole('button', { name: 'Aprobar', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Emitir', exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Emitir', exact: true }).click();
    await expect(page.getByText('Emitida').first()).toBeVisible();

    await page.getByRole('button', { name: 'Anular', exact: true }).click();
    await page.getByLabel('Motivo').fill('Orden duplicada');
    await page.getByRole('button', { name: 'Confirmar anulación' }).click();
    await expect(page.getByText('Cancelada').first()).toBeVisible();
    await expectNoHorizontalOverflow(page);
  });
});
