import type { Page, Route } from '@playwright/test';

const session = {
  accessToken: 'e2e-access-token',
  refreshToken: 'e2e-refresh-token',
  tokenType: 'Bearer',
  accessExpiresAt: '2099-01-01T00:00:00-05:00',
  refreshExpiresAt: '2099-01-01T00:00:00-05:00',
  tenantId: '11111111-1111-1111-1111-111111111111',
  userId: '22222222-2222-2222-2222-222222222222',
  sessionId: '33333333-3333-3333-3333-333333333333',
  passwordChangeRequired: false
};

const dashboardSummary = {
  salesToday: 8420.5,
  transactionsToday: 48,
  stockUnits: 4286,
  lowStockProducts: 12,
  expiringLots: 8,
  activeCustomers: 326,
  asOf: '2026-08-31T12:00:00-05:00'
};

export const json = (route: Route, status: number, body: unknown) =>
  route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });

export async function mockAuthApi(page: Page): Promise<void> {
  await page.route('**/api/v1/auth/login', (route) => json(route, 200, session));
  await page.route('**/api/v1/auth/refresh', (route) => json(route, 200, session));
  await page.route('**/api/v1/auth/logout', (route) => route.fulfill({ status: 204 }));
  await page.route('**/api/v1/dashboard/summary', (route) => json(route, 200, dashboardSummary));
}

export async function login(page: Page): Promise<void> {
  await mockAuthApi(page);
  await page.goto('/login');
  await page.getByLabel('Usuario o correo electrónico').fill('maria.rojas@boticas.pe');
  await page.getByLabel('Contraseña', { exact: true }).fill('Boticas2026!');
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();
  await page.waitForURL('**/dashboard');
}
