import { expect, test } from '@playwright/test';
import { login } from './support/login';

test.describe('Dropdown de perfil', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('abre al hacer clic en el trigger y muestra los grupos', async ({ page }) => {
    await page.getByRole('button', { name: /María Rojas/ }).click();

    await expect(page.getByRole('menuitem', { name: 'Mi Perfil' })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: 'Seguridad' })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: 'Configuraciones' })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: 'Cambiar sucursal' })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: 'Ayuda y soporte' })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: 'Cerrar sesión' })).toBeVisible();
  });

  test('cierra con la tecla Escape', async ({ page }) => {
    await page.getByRole('button', { name: /María Rojas/ }).click();
    await expect(page.getByRole('menuitem', { name: 'Mi Perfil' })).toBeVisible();

    await page.keyboard.press('Escape');

    await expect(page.getByRole('menuitem', { name: 'Mi Perfil' })).toBeHidden();
  });

  test('cierra al hacer clic afuera', async ({ page }) => {
    await page.getByRole('button', { name: /María Rojas/ }).click();
    await expect(page.getByRole('menuitem', { name: 'Mi Perfil' })).toBeVisible();

    await page.mouse.click(10, 10);

    await expect(page.getByRole('menuitem', { name: 'Mi Perfil' })).toBeHidden();
  });

  test('navega a la pagina placeholder al hacer clic en Mi Perfil', async ({ page }) => {
    await page.getByRole('button', { name: /María Rojas/ }).click();
    await page.getByRole('menuitem', { name: 'Mi Perfil' }).click();

    await expect(page).toHaveURL(/\/perfil$/);
    await expect(page.getByRole('heading', { name: 'Mi Perfil' })).toBeVisible();
  });

  test('cierra sesion y redirige a login', async ({ page }) => {
    await page.getByRole('button', { name: /María Rojas/ }).click();
    await page.getByRole('menuitem', { name: 'Cerrar sesión' }).click();

    await expect(page).toHaveURL(/\/login$/);
  });

  test('navegable por teclado: Enter abre el menu y Enter navega al item resaltado', async ({
    page
  }) => {
    const trigger = page.getByRole('button', { name: /María Rojas/ });
    await trigger.focus();
    await page.keyboard.press('Enter');

    await expect(page.getByRole('menuitem', { name: 'Mi Perfil' })).toBeVisible();

    // Radix DropdownMenu resalta el primer item ("Mi Perfil") automáticamente
    // al abrir con teclado. Enter lo activa y navega a su ruta — se verifica
    // el resultado end-to-end (la navegación) en vez de un atributo interno
    // de highlight de Radix.
    await page.keyboard.press('Enter');

    await expect(page).toHaveURL(/\/perfil$/);
    await expect(page.getByRole('heading', { name: 'Mi Perfil' })).toBeVisible();
  });
});
