import type { Page } from '@playwright/test';

export async function login(page: Page): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('Usuario o correo electrónico').fill('maria.rojas@boticas.pe');
  await page.getByLabel('Contraseña', { exact: true }).fill('Boticas2026!');
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();
  await page.waitForURL('**/dashboard');
}
