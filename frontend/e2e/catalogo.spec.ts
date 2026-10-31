import { expect, test } from '@playwright/test';
import { json, login } from './support/login';
import { expectNoHorizontalOverflow } from './support/layout';
import { SKU_VACIO } from '../apps/erp-web/src/features/catalogo/lib/sku-form';
import { PRODUCTO_REGULADO_VACIO } from '../apps/erp-web/src/features/catalogo/lib/producto-regulado-form';

for (const sku of [true, false]) {
  test(`conserva filtros al volver del detalle de ${sku ? 'SKU' : 'producto regulado'}`, async ({
    page
  }) => {
    const base = '/catalogo/' + (sku ? 'skus' : 'productos-regulados');
    const record = {
      ...(sku ? SKU_VACIO : PRODUCTO_REGULADO_VACIO),
      id: 'registro',
      codigoInterno: 'SKU-1',
      descripcionComercial: 'Producto prueba',
      denominacion: 'Producto prueba',
      estado: 'ACTIVO',
      estadoRegulatorio: 'VIGENTE',
      codigosBarra: [],
      principiosActivos: []
    };
    await page.route('**/api/v1/catalogo/**', (route) => {
      const path = new URL(route.request().url()).pathname;
      if (path === '/api/v1' + base + '/registro') return json(route, 200, record);
      if (path === '/api/v1' + base)
        return json(route, 200, { items: [record], page: 0, size: 20, totalElements: 1 });
      return json(
        route,
        200,
        ['marcas', 'categorias', 'productos-regulados'].some((name) => path.endsWith('/' + name))
          ? { items: [], totalElements: 0 }
          : []
      );
    });
    await login(page);
    await page.goto(base);
    const search = page.getByRole('searchbox', {
      name: sku ? 'Buscar SKU' : 'Buscar producto regulado',
      exact: true
    });
    await search.fill('Producto');
    await expect(page).toHaveURL(/search=Producto/);
    const filteredUrl = page.url();
    await page.reload();
    await expect(search).toHaveValue('Producto');
    await page.getByRole('link', { name: /Ver detalle de/ }).click();
    await page
      .getByRole('link', {
        name: sku ? 'Catálogo / SKU' : 'Catálogo / Productos regulados',
        exact: true
      })
      .click();
    await expect(page).toHaveURL(filteredUrl);
    await expect(search).toHaveValue('Producto');
    await page.getByRole('button', { name: sku ? 'Nuevo SKU' : 'Nuevo producto regulado', exact: true }).click();
    await page.getByRole('button', { name: 'Cancelar', exact: true }).click();
    await expect(page).toHaveURL(filteredUrl);
    await expect(search).toHaveValue('Producto');
  });
}

for (const sku of [true, false]) {
  test(`crea y edita ${sku ? 'SKU' : 'producto regulado'} en páginas completas`, async ({
    page
  }, testInfo) => {
    const resource = sku ? 'skus' : 'productos-regulados';
    const title = sku ? 'SKU' : 'producto regulado';
    const field = sku ? 'Descripción comercial' : 'Denominación';
    const base = `/catalogo/${resource}`;
    let record: Record<string, unknown> | null = null;
    await page.route('**/api/v1/catalogo/**', (route) => {
      const path = new URL(route.request().url()).pathname;
      if (path === `/api/v1${base}` || path === `/api/v1${base}/registro-1`) {
        if (['POST', 'PUT'].includes(route.request().method())) {
          record = {
            ...(sku ? SKU_VACIO : PRODUCTO_REGULADO_VACIO),
            estado: 'ACTIVO',
            estadoRegulatorio: 'VIGENTE',
            codigosBarra: [],
            principiosActivos: [],
            ...route.request().postDataJSON(),
            id: 'registro-1'
          };
          return json(route, 200, record);
        }
        return json(
          route,
          200,
          path.endsWith('/registro-1')
            ? record
            : {
                items: record ? [record] : [],
                page: 0,
                size: 20,
                totalElements: record ? 1 : 0
              }
        );
      }
      if (
        ['marcas', 'categorias', 'productos-regulados'].some((name) => path.endsWith('/' + name))
      ) {
        return json(route, 200, { items: [], page: 0, size: 100, totalElements: 0 });
      }
      return json(
        route,
        200,
        path.endsWith('/unidades-medida')
          ? [{ codigo: 'UND', denominacion: 'Unidad', estado: 'ACTIVO' }]
          : []
      );
    });
    await login(page);
    await page.goto(base);
    await page.getByRole('button', { name: `Nuevo ${title}`, exact: true }).click();
    await expect(page).toHaveURL(new RegExp(base + '/nuevo$'));
    await expect(page.getByRole('dialog')).toHaveCount(0);
    if (sku) {
      await expect(
        page
          .getByLabel('Unidad de venta', { exact: true })
          .getByRole('option', { name: 'UND — Unidad', exact: true })
      ).toBeAttached();
      await page.getByLabel('Tipo de SKU', { exact: true }).selectOption('NO_REGULADO');
      await expect(page.getByLabel('Tipo de SKU', { exact: true })).toHaveValue('NO_REGULADO');
      await page.getByLabel('Código interno', { exact: true }).fill('SKU-TEST');
      await page.getByLabel('Unidad de venta', { exact: true }).selectOption('UND');
    } else {
      await page.getByLabel('Tipo de producto', { exact: true }).fill('FARMACEUTICO');
    }
    await page.getByLabel(field, { exact: true }).fill('Producto de prueba');
    if (sku)
      await expect(page.getByLabel('Tipo de SKU', { exact: true })).toHaveValue('NO_REGULADO');
    await expectNoHorizontalOverflow(page);
    await page.screenshot({ path: testInfo.outputPath(`${resource}-nuevo.png`), fullPage: true });
    await page.getByRole('button', { name: `Crear ${title}`, exact: true }).click();
    await expect(page).toHaveURL(new RegExp(base + '$'));
    await page.getByRole('link', { name: /Ver detalle de/ }).click();
    await page.getByRole('button', { name: 'Editar', exact: true }).click();
    await expect(page).toHaveURL(new RegExp(base + '/registro-1/editar$'));
    await page.reload();
    await expect(page.getByLabel(field, { exact: true })).toHaveValue('Producto de prueba');
    await page.getByLabel(field, { exact: true }).fill('Producto actualizado');
    await page.getByRole('button', { name: 'Guardar cambios', exact: true }).click();
    await expect(page).toHaveURL(new RegExp(base + '/registro-1$'));
    await expect(
      page.getByRole('heading', { name: 'Producto actualizado', exact: true })
    ).toBeVisible();
  });
}

test('permite encontrar catálogos y recuperar la navegación', async ({ page }, testInfo) => {
  await login(page);
  await page.goto('/catalogo');
  await expect(page.getByRole('heading', { name: 'Catálogo', exact: true })).toBeVisible();
  await expectNoHorizontalOverflow(page);
  await page.screenshot({ path: testInfo.outputPath('catalogo.png'), fullPage: true });
  await page.getByRole('searchbox', { name: 'Buscar catálogo' }).fill('categorias');
  await expect(page.getByRole('heading', { name: 'Categorías', exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Marcas', exact: true })).toBeHidden();
  await page.getByRole('button', { name: 'Limpiar búsqueda' }).click();
  await expect(page.getByRole('heading', { name: 'Marcas', exact: true })).toBeVisible();
});

test('mantiene los datos ante un error de guardado y permite cancelar', async ({
  page
}, testInfo) => {
  await page.route('**/api/v1/catalogo/marcas**', (route) =>
    route.request().method() === 'POST'
      ? json(route, 409, { detail: 'Ya existe una marca con este código.' })
      : json(route, 200, { items: [], page: 0, size: 20, totalElements: 0 })
  );
  await login(page);
  await page.goto('/catalogo/marcas');
  await page.getByRole('button', { name: 'Nueva marca' }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel('Código', { exact: true }).fill('MARCA01');
  await dialog.getByLabel('Nombre', { exact: true }).fill('Marca de prueba');
  await dialog.getByRole('button', { name: 'Crear marca' }).click();
  await expect(dialog.getByRole('alert')).toContainText('Ya existe una marca');
  await expect(dialog.getByLabel('Nombre', { exact: true })).toHaveValue('Marca de prueba');
  await expectNoHorizontalOverflow(page);
  await page.screenshot({ path: testInfo.outputPath('marca-error.png'), fullPage: true });
  await dialog.getByRole('button', { name: 'Cancelar' }).click();
  await expect(dialog).toBeHidden();
});

test('organiza el detalle de SKU y mantiene accesibles las acciones del formulario largo', async ({
  page
}, testInfo) => {
  await page.route('**/api/v1/catalogo/**', (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith('/skus/sku-1')) {
      return json(route, 200, {
        ...SKU_VACIO,
        id: 'sku-1',
        tipoSku: 'NO_REGULADO',
        codigoInterno: 'SKU-001',
        descripcionComercial: 'Producto comercial de prueba',
        estado: 'ACTIVO',
        categoriaId: null,
        marcaId: null,
        productoReguladoId: null,
        precioVentaReferencia: null,
        codigosBarra: []
      });
    }
    if (path.endsWith('/unidades-medida')) {
      return json(route, 200, [{ codigo: 'UND', denominacion: 'Unidad', estado: 'ACTIVO' }]);
    }
    return json(route, 200, { items: [], page: 0, size: 100, totalElements: 0 });
  });
  await login(page);
  await page.goto('/catalogo/skus/sku-1');
  await expect(page.getByRole('heading', { name: 'Identificación comercial' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Control, stock y precio' })).toBeAttached();
  await expectNoHorizontalOverflow(page);
  await page.screenshot({ path: testInfo.outputPath('sku-detalle.png'), fullPage: true });
  await page.getByRole('button', { name: 'Editar', exact: true }).click();
  await expect(page).toHaveURL(/\/catalogo\/skus\/sku-1\/editar$/);
  await expect(page.getByRole('dialog')).toHaveCount(0);
  const dialog = page.getByRole('main');
  await expect(dialog.getByLabel('Código interno')).toHaveValue('SKU-001');
  await expect(dialog.getByRole('button', { name: 'Guardar cambios' })).toBeInViewport();
  await dialog.getByLabel('Stock mínimo por defecto').scrollIntoViewIfNeeded();
  await expect(dialog.getByRole('button', { name: 'Cancelar' })).toBeInViewport();
  await expectNoHorizontalOverflow(page);
  await page.screenshot({ path: testInfo.outputPath('sku-formulario.png'), fullPage: true });
  await dialog.getByRole('button', { name: 'Cancelar' }).click();
  await expect(page).toHaveURL(/\/catalogo\/skus\/sku-1$/);
});
