import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina, sampleEmpresa, sampleEstablecimiento } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { EmpresaDetailPage } from './EmpresaDetailPage';

const detailUrl = '*/api/v1/organizacion/empresas/empresa-1';
const statusUrl = '*/api/v1/organizacion/empresas/empresa-1/estado';

function mockDefaultHandlers() {
  server.use(
    http.get(detailUrl, () => HttpResponse.json(sampleEmpresa)),
    http.get('*/api/v1/organizacion/establecimientos', () =>
      HttpResponse.json(pagina([sampleEstablecimiento]))
    )
  );
}

function renderPage() {
  return renderRoute(
    '/organizacion/empresas/:empresaId',
    EmpresaDetailPage,
    '/organizacion/empresas/empresa-1'
  );
}

describe('EmpresaDetailPage', () => {
  it('muestra los datos de la empresa y sus establecimientos', async () => {
    mockDefaultHandlers();

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Boticas SAC' })).toBeInTheDocument();
    expect(screen.getByText('20123456786')).toBeInTheDocument();
    expect(screen.getByText('PEN')).toBeInTheDocument();
    expect(screen.getByText('America/Lima')).toBeInTheDocument();
    expect(screen.getAllByText('—').length).toBeGreaterThan(0);
    expect(screen.getByText('No')).toBeInTheDocument();
    expect(screen.getAllByText('ACTIVO').length).toBeGreaterThan(0);
    expect(screen.getByRole('link', { name: 'Organización / Empresas' })).toHaveAttribute(
      'href',
      '/organizacion/empresas'
    );
    expect(await screen.findByRole('link', { name: 'Botica Central' })).toBeInTheDocument();
  });

  it('muestra el estado de carga inicial', () => {
    mockDefaultHandlers();

    renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
  });

  it('informa cuando la empresa no existe', async () => {
    server.use(
      http.get(detailUrl, () =>
        HttpResponse.json(
          { title: 'Not Found', detail: 'La empresa indicada no existe.' },
          { status: 404 }
        )
      )
    );

    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El recurso no existe o no pertenece a tu organización.'
    );
  });

  it('edita la empresa sin enviar el RUC', async () => {
    mockDefaultHandlers();
    let query = new URLSearchParams();
    let body: Record<string, unknown> = {};
    server.use(
      http.put(detailUrl, async ({ request }) => {
        query = new URL(request.url).searchParams;
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...sampleEmpresa, razonSocial: 'Boticas del Perú SAC' });
      })
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Boticas SAC' });

    await user.click(screen.getByRole('button', { name: 'Editar' }));
    expect(screen.getByLabelText('RUC')).toHaveAttribute('readonly');
    const razonSocial = screen.getByLabelText('Razón social');
    await user.clear(razonSocial);
    await user.type(razonSocial, 'Boticas del Perú SAC');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(query.get('tenantId')).toBe('tenant-1');
    expect(body).toMatchObject({ razonSocial: 'Boticas del Perú SAC', monedaFuncional: 'PEN' });
    expect('ruc' in body).toBe(false);
  });

  it('muestra el error del servidor al editar y lo limpia al cerrar', async () => {
    mockDefaultHandlers();
    server.use(
      http.put(detailUrl, () =>
        HttpResponse.json(
          { title: 'Bad Request', detail: 'La razón social no es válida.' },
          { status: 400 }
        )
      )
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Boticas SAC' });

    await user.click(screen.getByRole('button', { name: 'Editar' }));
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('La razón social no es válida.');
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    await user.click(screen.getByRole('button', { name: 'Editar' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('cambia el estado de la empresa', async () => {
    mockDefaultHandlers();
    let body: unknown;
    server.use(
      http.patch(statusUrl, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ ...sampleEmpresa, estado: 'SUSPENDIDO' });
      })
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Boticas SAC' });

    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));
    await user.selectOptions(screen.getByLabelText('Estado'), 'SUSPENDIDO');
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(body).toEqual({ estado: 'SUSPENDIDO' });
  });

  it('muestra el error al cambiar el estado y lo limpia al cerrar', async () => {
    mockDefaultHandlers();
    server.use(
      http.patch(statusUrl, () => HttpResponse.json({ title: 'Forbidden' }, { status: 403 }))
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Boticas SAC' });

    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));
    await user.selectOptions(screen.getByLabelText('Estado'), 'BLOQUEADO');
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No tienes permiso para esta acción.'
    );
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
