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

  test('crea una orden de compra con IGV sugerido', async ({ page }) => {
    await abrirComprasEn(page, '/compras/ordenes');

    await page.getByRole('link', { name: 'Nueva orden' }).click();
    await expect(page.getByRole('heading', { name: 'Nueva orden de compra' })).toBeVisible();
    await page.getByLabel('Proveedor').selectOption('prov-1');
    await page.getByLabel('Establecimiento de destino').selectOption('est-1');
    await page.getByLabel('Buscar producto').fill('paracetamol');
    await page.getByRole('button', { name: 'Buscar' }).click();
    await page.getByRole('button', { name: 'Agregar MED-001' }).click();
    await page.getByLabel('Precio de MED-001').fill('5.5');
    await expect(page.getByLabel('Impuesto de MED-001')).toHaveValue('0.99');
    await expectNoHorizontalOverflow(page);

    await page.getByRole('button', { name: 'Crear orden' }).click();

    await expect(page.getByRole('heading', { name: 'Orden OC-2026-000001' })).toBeVisible();
  });

  test('aprueba, emite y registra una recepción parcial', async ({ page }) => {
    await abrirComprasEn(page, '/compras/ordenes/orden-1');

    await expect(page.getByRole('heading', { name: 'Orden OC-2026-000001' })).toBeVisible();
    await page.getByRole('button', { name: 'Aprobar', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Emitir', exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Emitir', exact: true }).click();
    await expect(page.getByText('La orden aún no tiene recepciones.')).toBeVisible();

    await page.getByRole('link', { name: 'Registrar recepción' }).click();
    await expect(
      page.getByRole('heading', { name: 'Recepción de la orden OC-2026-000001' })
    ).toBeVisible();
    await page.getByLabel('Almacén', { exact: true }).selectOption('alm-1');
    await page.getByLabel('Serie del documento').fill('F001');
    await page.getByLabel('Número del documento').fill('123');
    const linea = page.getByRole('group', { name: 'Línea 1 — Paracetamol 500 mg' });
    await expect(linea.getByText('Pendiente: 10 UND')).toBeVisible();
    await expect(linea.getByLabel('Costo unitario')).toHaveValue('5.5');
    await linea.getByLabel('Número de lote').fill('L2026-01');
    await linea.getByLabel('Fecha de vencimiento').fill('2099-12-31');
    await linea.getByLabel('Cantidad recibida').fill('4');
    await expectNoHorizontalOverflow(page);

    await page.getByRole('button', { name: 'Registrar recepción' }).click();

    await expect(page.getByRole('heading', { name: 'Orden OC-2026-000001' })).toBeVisible();
    await expect(page.getByText('Parcialmente recibida').first()).toBeVisible();
    await expect(page.getByText('REC-2026-000001')).toBeVisible();
    await expect(page.getByText('L2026-01')).toBeVisible();
    await expectNoHorizontalOverflow(page);
  });
});
