import { expect, test } from '@playwright/test';
import { abrirInventarioEn } from './support/inventario-api';
import { expectNoHorizontalOverflow } from './support/layout';

test.describe('Inventario', () => {
  test('lista las posiciones y las filtra por almacén', async ({ page }) => {
    await abrirInventarioEn(page, '/inventario');

    await expect(page.getByRole('heading', { name: 'Inventario', exact: true })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Ver detalle del lote L001' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Ver detalle del lote L002' })).toBeVisible();
    await expectNoHorizontalOverflow(page);

    await page.getByLabel('Almacén', { exact: true }).selectOption('alm-3');

    await expect(page.getByRole('link', { name: 'Ver detalle del lote L002' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Ver detalle del lote L001' })).toBeHidden();
    await expect(page).toHaveURL(/almacenId=alm-3/);
  });

  test('registra un ingreso con lote nuevo desde la cabecera', async ({ page }) => {
    await abrirInventarioEn(page, '/inventario');

    await page.getByRole('button', { name: 'Registrar ingreso' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByLabel('Almacén', { exact: true }).selectOption('alm-1');
    await dialog
      .getByLabel('SKU', { exact: true })
      .selectOption({ label: 'MED-001 — Paracetamol 500 mg' });
    await dialog.getByLabel('Número de lote').fill('L-NEW');
    await dialog.getByLabel('Fecha de vencimiento').fill('2030-01-01');
    await dialog.getByLabel('Cantidad').fill('12');
    await dialog.getByLabel('Motivo').fill('Ingreso inicial');
    await dialog.getByRole('button', { name: 'Registrar ingreso' }).click();

    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(page.getByRole('link', { name: 'Ver detalle del lote L-NEW' })).toBeVisible();
  });

  test('bloquea y desbloquea un lote desde su detalle', async ({ page }) => {
    await abrirInventarioEn(page, '/inventario');

    await page.getByRole('link', { name: 'Ver detalle del lote L001' }).click();
    await expect(page.getByRole('heading', { name: 'Lote L001' })).toBeVisible();
    await expectNoHorizontalOverflow(page);

    await page.getByRole('button', { name: 'Bloquear lote' }).click();
    await page.getByRole('dialog').getByLabel('Motivo').fill('Control de calidad');
    await page.getByRole('dialog').getByRole('button', { name: 'Bloquear lote' }).click();

    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(page.getByText('BLOQUEADO', { exact: true })).toBeVisible();
    await expect(page.getByText('Control de calidad')).toBeVisible();

    await page.getByRole('button', { name: 'Desbloquear lote' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Desbloquear lote' }).click();

    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(page.getByText('HABILITADO', { exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Bloquear lote' })).toBeVisible();
  });
});
