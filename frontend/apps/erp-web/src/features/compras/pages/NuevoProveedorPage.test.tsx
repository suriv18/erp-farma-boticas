import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleProveedor } from '../../../test/compras-fixtures';
import { renderRoute } from '../../../test/render-route';
import { NuevoProveedorPage } from './NuevoProveedorPage';

const proveedoresUrl = '*/api/v1/compras/proveedores';

function renderPage() {
  return renderRoute('*', NuevoProveedorPage, '/compras/proveedores/nuevo');
}

async function completar(user: ReturnType<typeof renderPage>['user']) {
  await user.type(screen.getByLabelText('Número de documento'), '20100070970');
  await user.type(screen.getByLabelText('Razón social'), 'Laboratorios Perú SAC');
}

describe('NuevoProveedorPage', () => {
  it('crea el proveedor, invalida las consultas y abre su detalle', async () => {
    let body: unknown;
    server.use(
      http.post(proveedoresUrl, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(sampleProveedor, { status: 201 });
      })
    );
    const { user, router, queryClient } = renderPage();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');

    await completar(user);
    await user.click(screen.getByRole('button', { name: 'Crear proveedor' }));

    await waitFor(() => expect(router.state.location.pathname).toBe('/compras/proveedores/prov-1'));
    expect(body).toMatchObject({
      tipoDocumento: '6',
      numeroDocumento: '20100070970',
      razonSocial: 'Laboratorios Perú SAC',
      diasCreditoDefault: 0,
      monedaDefault: 'PEN',
      esDistribuidor: true
    });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['compras'] });
  });

  it('muestra el error traducido del backend y no navega', async () => {
    server.use(
      http.post(proveedoresUrl, () =>
        HttpResponse.json(
          { title: 'Conflicto', code: 'COM_PROVEEDOR_DUPLICADO', detail: 'duplicado' },
          { status: 409 }
        )
      )
    );
    const { user, router } = renderPage();

    await completar(user);
    await user.click(screen.getByRole('button', { name: 'Crear proveedor' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Ya existe un proveedor con ese documento.'
    );
    expect(router.state.location.pathname).toBe('/compras/proveedores/nuevo');
  });

  it('cancela y vuelve al listado', async () => {
    const { user, router } = renderPage();

    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(router.state.location.pathname).toBe('/compras/proveedores');
  });
});
