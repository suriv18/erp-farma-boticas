import { expect, test } from '@playwright/test';
import { json, login } from './support/login';

test('pagina tablas y adapta los controles sin desbordamiento', async ({ page }, testInfo) => {
  await page.route('**/api/v1/catalogo/marcas**', (route) => {
    const url = new URL(route.request().url());
    const currentPage = Number(url.searchParams.get('page') ?? 0);
    const size = Number(url.searchParams.get('size') ?? 20);
    return json(route, 200, {
      items: Array.from({ length: size }, (_, index) => ({
        id: String(currentPage * size + index),
        codigo: `MAR-${currentPage * size + index}`,
        nombre: `Marca ${currentPage * size + index + 1}`,
        descripcion: null,
        estado: 'ACTIVO'
      })),
      page: currentPage,
      size,
      totalElements: 700
    });
  });
  await login(page);
  await page.goto('/catalogo/marcas');
  const pagination = page.getByRole('navigation', { name: 'Paginación' });
  await expect(pagination.getByRole('status')).toContainText('1–20 de 700');
  for (const width of [320, 768, 1024, 1280, 1440]) {
    await page.setViewportSize({ width, height: 900 });
    await pagination.scrollIntoViewIfNeeded();
    expect(await pagination.evaluate((element) => element.scrollWidth <= element.clientWidth)).toBe(
      true
    );
    const current = pagination.getByRole('button', { name: 'Página 1', exact: true });
    if (width >= 1024) await expect(current).toBeVisible();
    else await expect(pagination.getByText('Página 1 de 35')).toBeVisible();
    await pagination.screenshot({ path: testInfo.outputPath(`pagination-${width}.png`) });
  }
  await pagination.getByRole('button', { name: 'Página 35', exact: true }).click();
  await expect(pagination.getByRole('status')).toContainText('681–700 de 700');
  await expect(pagination.getByRole('button', { name: 'Siguiente' })).toBeDisabled();
  await pagination.getByRole('combobox', { name: 'Filas por página' }).selectOption('50');
  await expect(pagination.getByRole('status')).toContainText('1–50 de 700');
  await expect(pagination.getByRole('button', { name: 'Página 1', exact: true })).toHaveAttribute(
    'aria-current',
    'page'
  );
});
