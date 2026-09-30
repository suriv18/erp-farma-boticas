import { expect, test, type Page } from '@playwright/test';
import { abrirSesionEn } from './support/organizacion-api';

async function expectNoHorizontalOverflow(page: Page) {
  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth - window.innerWidth
  );
  expect(overflow).toBeLessThanOrEqual(1);
}

async function crearEmpresa(page: Page, ruc: string, razonSocial: string) {
  await page.getByRole('button', { name: 'Nueva empresa' }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel('RUC').fill(ruc);
  await dialog.getByLabel('Razón social').fill(razonSocial);
  await dialog.getByRole('button', { name: 'Crear empresa' }).click();
}

test.describe('Organización', () => {
  test('el resumen enlaza con la gestión de empresas', async ({ page }) => {
    await abrirSesionEn(page, '/organizacion');

    await page.getByRole('link', { name: 'Gestionar empresas' }).click();

    await expect(page).toHaveURL(/\/organizacion\/empresas$/);
    await expect(page.getByRole('heading', { name: 'Empresas', exact: true })).toBeVisible();
    await expectNoHorizontalOverflow(page);
  });

  test('crea una empresa, filtra el listado y rechaza un RUC duplicado', async ({ page }) => {
    await abrirSesionEn(page, '/organizacion/empresas');
    await expect(page.getByText('Aún no hay empresas registradas.')).toBeVisible();

    await crearEmpresa(page, '20999999990', 'PRUEBA UI Boticas SAC');

    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(page.getByRole('link', { name: 'PRUEBA UI Boticas SAC' })).toBeVisible();
    await expectNoHorizontalOverflow(page);

    await page.getByLabel('Buscar empresa').fill('no-existe');
    await expect(page.getByText('Aún no hay empresas registradas.')).toBeVisible();
    await page.getByLabel('Buscar empresa').fill('PRUEBA');
    await expect(page.getByRole('link', { name: 'PRUEBA UI Boticas SAC' })).toBeVisible();

    await crearEmpresa(page, '20999999990', 'Otra razón social');

    await expect(page.getByRole('alert')).toContainText(
      'Ya existe una empresa con el RUC indicado.'
    );
    await page.getByRole('button', { name: 'Cerrar' }).click();
    await expect(page.getByRole('dialog')).toBeHidden();
  });

  test('cancela la creación de una empresa sin guardarla', async ({ page }) => {
    await abrirSesionEn(page, '/organizacion/empresas');

    await page.getByRole('button', { name: 'Nueva empresa' }).click();
    const dialog = page.getByRole('dialog');
    const crear = dialog.getByRole('button', { name: 'Crear empresa' });
    const cancelar = dialog.getByRole('button', { name: 'Cancelar' });
    const [crearBox, cancelarBox] = await Promise.all([
      crear.boundingBox(),
      cancelar.boundingBox()
    ]);
    expect(cancelarBox?.x).toBeGreaterThan((crearBox?.x ?? 0) + (crearBox?.width ?? 0) - 1);

    await dialog.getByLabel('RUC').fill('20999999990');
    await cancelar.click();

    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(page.getByText('Aún no hay empresas registradas.')).toBeVisible();
  });

  test('rechaza un RUC con dígito verificador inválido sin cerrar el formulario', async ({
    page
  }) => {
    await abrirSesionEn(page, '/organizacion/empresas');

    await crearEmpresa(page, '20123456789', 'Empresa con RUC inválido');

    await expect(page.getByRole('dialog').getByRole('alert')).toContainText(
      'El RUC no es válido: el dígito verificador no coincide.'
    );
    await expect(page.getByRole('dialog')).toBeVisible();
  });

  test('gestiona empresa, establecimiento, almacén y terminal de punta a punta', async ({
    page
  }) => {
    test.setTimeout(90_000);
    await abrirSesionEn(page, '/organizacion/empresas');
    await crearEmpresa(page, '20999999990', 'PRUEBA UI Boticas SAC');
    await page.getByRole('link', { name: 'PRUEBA UI Boticas SAC' }).click();

    await expect(
      page.getByRole('heading', { name: 'PRUEBA UI Boticas SAC', exact: true })
    ).toBeVisible();
    await expectNoHorizontalOverflow(page);

    await page.getByRole('button', { name: 'Editar', exact: true }).click();
    const editarEmpresa = page.getByRole('dialog');
    await expect(editarEmpresa.getByLabel('RUC')).toHaveAttribute('readonly', '');
    await editarEmpresa.getByLabel('Nombre comercial').fill('UI Boticas');
    await editarEmpresa.getByRole('button', { name: 'Guardar cambios' }).click();
    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(page.getByText('UI Boticas', { exact: true })).toBeVisible();

    const aviso = page.getByRole('status').filter({ hasText: 'no admite' });
    await page.getByRole('button', { name: 'Cambiar estado' }).click();
    await page.getByRole('dialog').getByLabel('Estado').selectOption('SUSPENDIDO');
    await expect(page.getByRole('button', { name: 'Guardar estado' })).toBeDisabled();
    await page.getByRole('dialog').getByLabel('Entiendo las consecuencias').check();
    await page.getByRole('button', { name: 'Guardar estado' }).click();
    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(page.getByText('SUSPENDIDO', { exact: true })).toBeVisible();
    await expect(aviso).toContainText(
      'La empresa está suspendida; no admite establecimientos nuevos.'
    );
    await expect(page.getByRole('button', { name: 'Nuevo establecimiento' })).toBeDisabled();

    await page.getByRole('button', { name: 'Cambiar estado' }).click();
    await page.getByRole('dialog').getByLabel('Estado').selectOption('ACTIVO');
    await page.getByRole('button', { name: 'Guardar estado' }).click();
    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(aviso).toBeHidden();

    await page.getByRole('button', { name: 'Nuevo establecimiento' }).click();
    const nuevoEstablecimiento = page.getByRole('dialog');
    await nuevoEstablecimiento.getByLabel('Código', { exact: true }).fill('UI-001');
    await nuevoEstablecimiento.getByLabel('Nombre', { exact: true }).fill('Botica UI Central');
    await nuevoEstablecimiento.getByRole('button', { name: 'Crear establecimiento' }).click();
    await expect(page.getByRole('dialog')).toBeHidden();
    await page.getByRole('link', { name: 'Botica UI Central' }).click();

    await expect(
      page.getByRole('heading', { name: 'Botica UI Central', exact: true })
    ).toBeVisible();
    await expectNoHorizontalOverflow(page);

    await page.getByRole('button', { name: 'Cambiar estado' }).click();
    await page.getByRole('dialog').getByLabel('Estado').selectOption('REMODELACION');
    await page.getByRole('button', { name: 'Guardar estado' }).click();
    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(page.getByText('REMODELACION', { exact: true })).toBeVisible();

    await page.getByRole('button', { name: 'Nuevo almacén' }).click();
    const nuevoAlmacen = page.getByRole('dialog');
    await nuevoAlmacen.getByLabel('Código', { exact: true }).fill('UI-ALM');
    await nuevoAlmacen.getByLabel('Nombre', { exact: true }).fill('Almacén UI');
    await nuevoAlmacen.getByLabel('Tipo de almacén').selectOption('REFRIGERADO');
    await expect(nuevoAlmacen.getByLabel('Controla temperatura')).toBeChecked();
    await nuevoAlmacen.getByLabel('Temperatura mínima (°C)').fill('2');
    await nuevoAlmacen.getByLabel('Temperatura máxima (°C)').fill('8');
    await nuevoAlmacen.getByRole('button', { name: 'Crear almacén' }).click();
    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(page.getByText('REFRIGERADO', { exact: true })).toBeVisible();

    await page.getByRole('button', { name: 'Editar Almacén UI' }).click();
    const editarAlmacen = page.getByRole('dialog');
    await expect(editarAlmacen.getByLabel('Código', { exact: true })).toHaveAttribute(
      'readonly',
      ''
    );
    await editarAlmacen.getByLabel('Almacén activo').uncheck();
    await editarAlmacen.getByRole('button', { name: 'Guardar cambios' }).click();
    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(page.getByText('INACTIVO', { exact: true })).toBeVisible();

    await page.getByRole('button', { name: 'Nuevo terminal' }).click();
    const nuevoTerminal = page.getByRole('dialog');
    await nuevoTerminal.getByLabel('Código', { exact: true }).fill('UI-POS');
    await nuevoTerminal.getByLabel('Nombre', { exact: true }).fill('Caja UI');
    await nuevoTerminal.getByLabel('Serie de boleta').fill('B001');
    await nuevoTerminal.getByLabel('Serie de factura').fill('F001');
    await nuevoTerminal.getByLabel('Dirección IP').fill('10.0.0.77');
    await nuevoTerminal.getByRole('button', { name: 'Crear terminal' }).click();
    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(page.getByText('10.0.0.77')).toBeVisible();

    await page.getByRole('button', { name: 'Editar Caja UI' }).click();
    await page.getByRole('dialog').getByLabel('Estado').selectOption('MANTENIMIENTO');
    await page.getByRole('dialog').getByRole('button', { name: 'Guardar cambios' }).click();
    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(page.getByText('MANTENIMIENTO', { exact: true })).toBeVisible();

    await page.getByRole('button', { name: 'Nuevo terminal' }).click();
    const terminalRepetido = page.getByRole('dialog');
    await terminalRepetido.getByLabel('Código', { exact: true }).fill('UI-POS2');
    await terminalRepetido.getByLabel('Nombre', { exact: true }).fill('Caja UI 2');
    await terminalRepetido.getByLabel('Serie de boleta').fill('b001');
    await terminalRepetido.getByLabel('Serie de factura').fill('f002');
    await terminalRepetido.getByRole('button', { name: 'Crear terminal' }).click();
    await expect(terminalRepetido.getByRole('alert')).toContainText(
      'La serie B001 ya está asignada a otra caja de esta empresa.'
    );
    await terminalRepetido.getByRole('button', { name: 'Cerrar' }).click();
    await expect(page.getByRole('dialog')).toBeHidden();
    await expectNoHorizontalOverflow(page);
  });

  test('los formularios validan antes de enviar', async ({ page }) => {
    await abrirSesionEn(page, '/organizacion/empresas');

    await page.getByRole('button', { name: 'Nueva empresa' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Crear empresa' }).click();

    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.getByRole('dialog').getByRole('alert').first()).toBeVisible();
  });
});
