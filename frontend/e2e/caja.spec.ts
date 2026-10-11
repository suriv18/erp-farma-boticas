import { expect, test } from '@playwright/test';
import { abrirCajaEn } from './support/caja-api';
import { expectNoHorizontalOverflow } from './support/layout';

test.describe('Caja', () => {
  test('abre y cierra el turno de una terminal', async ({ page }) => {
    await abrirCajaEn(page, '/caja');

    await expect(page.getByRole('heading', { name: 'Caja', exact: true })).toBeVisible();
    await page.getByLabel('Establecimiento').selectOption('est-1');
    await page.getByLabel('Terminal').selectOption('term-1');
    await expect(page.getByText('No hay un turno abierto en esta terminal.')).toBeVisible();
    await expectNoHorizontalOverflow(page);

    await page.getByLabel('Fondo inicial').fill('100');
    await page.getByRole('button', { name: 'Abrir turno' }).click();
    await expect(page.getByRole('button', { name: 'Cerrar turno' })).toBeVisible();

    await page.getByLabel('Total declarado').fill('348.5');
    await page.getByLabel('Observación').fill('Faltante de monedas');
    await page.getByRole('button', { name: 'Cerrar turno' }).click();

    await expect(page.getByText('Turno cerrado')).toBeVisible();
    await expect(page.getByText('Faltante de monedas')).toBeVisible();
    await expectNoHorizontalOverflow(page);
  });
});
