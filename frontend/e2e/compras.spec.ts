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
});
