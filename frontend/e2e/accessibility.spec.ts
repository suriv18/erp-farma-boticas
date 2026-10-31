import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';
import { json, login } from './support/login';

test.beforeEach(async ({ page }) => {
  await page.route('**/api/v1/catalogo/**', (route) => {
    const path = new URL(route.request().url()).pathname;
    return json(
      route,
      200,
      ['marcas', 'categorias', 'productos-regulados'].some((name) => path.endsWith('/' + name))
        ? { items: [], page: 0, size: 100, totalElements: 0 }
        : []
    );
  });
  await login(page);
});

test('formularios de catálogo accesibles en tema claro y oscuro', async ({ page }) => {
  for (const theme of ['light', 'dark']) {
    for (const path of ['/catalogo/skus/nuevo', '/catalogo/productos-regulados/nuevo']) {
      await page.goto(path);
      await expect(page.locator('main form')).toBeVisible();
      await page.evaluate((value) => (document.documentElement.dataset.theme = value), theme);
      await page.evaluate(async () => {
        await Promise.all(
          document.getAnimations().map((animation) => animation.finished.catch(() => {}))
        );
      });
      await expect(page.getByText('Cargando opciones…')).toHaveCount(0);
      const results = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa'])
        .analyze();
      expect(results.violations, JSON.stringify(results.violations, null, 2)).toEqual([]);
    }
  }
});

test('menú móvil contiene el foco y vuelve al disparador con Escape', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByRole('button', { name: 'Abrir menú' }).click();
  const dialog = page.getByRole('dialog', { name: 'Menú principal' });
  await expect(dialog).toBeVisible();
  for (let i = 0; i < 14; i++) {
    await page.keyboard.press('Tab');
    expect(await dialog.evaluate((el) => el.contains(document.activeElement))).toBe(true);
  }
  await page.keyboard.press('Escape');
  await expect(dialog).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Abrir menú' })).toBeFocused();
});

test('protege los cambios del formulario al navegar y permite descartarlos', async ({ page }) => {
  await page.goto('/catalogo/productos-regulados/nuevo');
  await page.getByLabel('Denominación', { exact: true }).fill('Cambio pendiente');
  await page.getByRole('button', { name: 'Cancelar', exact: true }).click();
  await expect(page.getByRole('dialog', { name: 'Cambios sin guardar' })).toBeVisible();
  await page.getByRole('button', { name: 'Seguir editando' }).click();
  await expect(page.getByLabel('Denominación', { exact: true })).toHaveValue('Cambio pendiente');
  await page.getByRole('button', { name: 'Cancelar', exact: true }).click();
  await page.getByRole('button', { name: 'Descartar cambios' }).click();
  await expect(page).toHaveURL(/\/catalogo\/productos-regulados$/);
});
