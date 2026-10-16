import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleProveedor } from '../../../test/compras-fixtures';
import { renderRoute } from '../../../test/render-route';
import { ProveedorDetailPage } from './ProveedorDetailPage';

const detalleUrl = '*/api/v1/compras/proveedores/prov-1';
const estadoUrl = '*/api/v1/compras/proveedores/prov-1/estado';

function renderPage() {
  return renderRoute(
    '/compras/proveedores/:proveedorId',
    ProveedorDetailPage,
    '/compras/proveedores/prov-1'
  );
}

describe('ProveedorDetailPage', () => {
  it('muestra el proveedor con su estado y el formulario precargado', async () => {
    server.use(http.get(detalleUrl, () => HttpResponse.json(sampleProveedor)));

    renderPage();

    expect(
      await screen.findByRole('heading', { name: 'Laboratorios Perú SAC' })
    ).toBeInTheDocument();
    expect(screen.getByText('Documento 20100070970')).toBeInTheDocument();
    expect(screen.getByText('ACTIVO')).toBeInTheDocument();
    expect(screen.getByLabelText('Razón social')).toHaveValue('Laboratorios Perú SAC');
    expect(screen.getByLabelText('Laboratorio')).toBeChecked();
  });

  it('muestra la carga y el error de consulta', async () => {
    server.use(
      http.get(detalleUrl, () => HttpResponse.json({ title: 'No encontrado' }, { status: 404 }))
    );

    renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El recurso no existe o no pertenece a tu organización.'
    );
  });

  it('guarda los cambios, invalida las consultas y avisa', async () => {
    let body: unknown;
    server.use(
      http.get(detalleUrl, () => HttpResponse.json(sampleProveedor)),
      http.put(detalleUrl, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(sampleProveedor);
      })
    );
    const { user, queryClient } = renderPage();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');
    await screen.findByRole('heading', { name: 'Laboratorios Perú SAC' });

    await user.clear(screen.getByLabelText('Nombre comercial'));
    await user.type(screen.getByLabelText('Nombre comercial'), 'LabPerú 2');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByRole('status')).toHaveTextContent('Proveedor actualizado.');
    expect(body).toMatchObject({ nombreComercial: 'LabPerú 2', numeroDocumento: '20100070970' });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['compras'] });
  });

  it('muestra el error al guardar', async () => {
    server.use(
      http.get(detalleUrl, () => HttpResponse.json(sampleProveedor)),
      http.put(detalleUrl, () =>
        HttpResponse.json(
          { title: 'Conflicto', code: 'COM_PROVEEDOR_DUPLICADO', detail: 'duplicado' },
          { status: 409 }
        )
      )
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Laboratorios Perú SAC' });

    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Ya existe un proveedor con ese documento.'
    );
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('cambia el estado desde el diálogo y lo cierra', async () => {
    let body: unknown;
    server.use(
      http.get(detalleUrl, () => HttpResponse.json(sampleProveedor)),
      http.patch(estadoUrl, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ ...sampleProveedor, estado: 'SUSPENDIDO' });
      })
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Laboratorios Perú SAC' });

    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));
    await user.selectOptions(screen.getByLabelText('Estado'), 'SUSPENDIDO');
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));

    await waitFor(() => expect(body).toEqual({ estado: 'SUSPENDIDO' }));
    await waitFor(() =>
      expect(
        screen.queryByRole('heading', { name: 'Cambiar estado de Laboratorios Perú SAC' })
      ).not.toBeInTheDocument()
    );
  });

  it('muestra el error del cambio de estado y permite cancelar el diálogo', async () => {
    server.use(
      http.get(detalleUrl, () => HttpResponse.json(sampleProveedor)),
      http.patch(estadoUrl, () => HttpResponse.json({ title: 'Sin permiso' }, { status: 403 }))
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Laboratorios Perú SAC' });

    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));
    await user.selectOptions(screen.getByLabelText('Estado'), 'BLOQUEADO');
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));

    expect(await screen.findByText('No tienes permiso para esta acción.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(
      screen.queryByRole('heading', { name: 'Cambiar estado de Laboratorios Perú SAC' })
    ).not.toBeInTheDocument();
  });
});
