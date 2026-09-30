import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina, sampleEmpresa } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { EmpresasPage } from './EmpresasPage';

const listUrl = '*/api/v1/organizacion/empresas';

function renderPage() {
  return renderRoute('/organizacion/empresas', EmpresasPage, '/organizacion/empresas');
}

describe('EmpresasPage', () => {
  it('lista las empresas con enlace al detalle, nombre comercial y estado', async () => {
    server.use(
      http.get(listUrl, () =>
        HttpResponse.json(
          pagina([
            sampleEmpresa,
            {
              ...sampleEmpresa,
              id: 'empresa-2',
              razonSocial: 'Inversiones Andinas SAC',
              nombreComercial: 'Andinas',
              estado: 'SUSPENDIDO'
            }
          ])
        )
      )
    );

    renderPage();

    const link = await screen.findByRole('link', { name: 'Boticas SAC' });
    expect(link).toHaveAttribute('href', '/organizacion/empresas/empresa-1');
    expect(screen.getByRole('link', { name: 'Inversiones Andinas SAC' })).toHaveAttribute(
      'href',
      '/organizacion/empresas/empresa-2'
    );
    expect(screen.getByText('—')).toBeInTheDocument();
    expect(screen.getByText('Andinas')).toBeInTheDocument();
    expect(screen.getByText('ACTIVO')).toBeInTheDocument();
    expect(screen.getByText('SUSPENDIDO')).toBeInTheDocument();
  });

  it('muestra el estado de carga y luego el mensaje de lista vacía', async () => {
    server.use(http.get(listUrl, () => HttpResponse.json(pagina([]))));

    renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(await screen.findByText('Aún no hay empresas registradas.')).toBeInTheDocument();
  });

  it('informa cuando no se puede cargar el listado', async () => {
    server.use(http.get(listUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 })));

    renderPage();

    expect(
      await screen.findByText('No se pudo cargar el listado de empresas.')
    ).toBeInTheDocument();
  });

  it('busca en el servidor, pagina y cambia el tamaño de página', async () => {
    let received = new URLSearchParams();
    server.use(
      http.get(listUrl, ({ request }) => {
        received = new URL(request.url).searchParams;
        return HttpResponse.json(
          pagina([sampleEmpresa], {
            page: Number(received.get('page')),
            size: Number(received.get('size')),
            totalElements: 120
          })
        );
      })
    );
    const { user } = renderPage();

    await screen.findByText('Boticas SAC');
    expect(received.get('tenantId')).toBe('tenant-1');
    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    await waitFor(() => expect(received.get('page')).toBe('1'));
    await user.selectOptions(screen.getByRole('combobox', { name: 'Filas por página' }), '50');
    await waitFor(() => {
      expect(received.get('size')).toBe('50');
      expect(received.get('page')).toBe('0');
    });
    await user.type(screen.getByLabelText('Buscar empresa'), 'bot');
    await waitFor(() => expect(received.get('search')).toBe('bot'));
    expect(received.get('page')).toBe('0');
  });

  it('crea una empresa y refresca el listado', async () => {
    let created: unknown;
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina(created ? [sampleEmpresa] : []))),
      http.post(listUrl, async ({ request }) => {
        created = await request.json();
        return HttpResponse.json(sampleEmpresa, { status: 201 });
      })
    );
    const { user } = renderPage();
    await screen.findByText('Aún no hay empresas registradas.');

    await user.click(screen.getByRole('button', { name: 'Nueva empresa' }));
    await user.type(screen.getByLabelText('RUC'), '20123456789');
    await user.type(screen.getByLabelText('Razón social'), 'Boticas SAC');
    await user.click(screen.getByRole('button', { name: 'Crear empresa' }));

    expect(await screen.findByRole('link', { name: 'Boticas SAC' })).toBeInTheDocument();
    expect(created).toMatchObject({
      tenantId: 'tenant-1',
      ruc: '20123456789',
      razonSocial: 'Boticas SAC',
      monedaFuncional: 'PEN'
    });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('cierra el modal con Cancelar sin crear la empresa', async () => {
    let posts = 0;
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina([]))),
      http.post(listUrl, () => {
        posts += 1;
        return HttpResponse.json(sampleEmpresa, { status: 201 });
      })
    );
    const { user } = renderPage();
    await screen.findByText('Aún no hay empresas registradas.');

    await user.click(screen.getByRole('button', { name: 'Nueva empresa' }));
    await user.type(screen.getByLabelText('RUC'), '20123456789');
    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(posts).toBe(0);
  });

  it('muestra el error del servidor al crear y lo limpia al cerrar el modal', async () => {
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina([]))),
      http.post(listUrl, () =>
        HttpResponse.json(
          { title: 'Conflict', detail: 'Ya existe una empresa con el RUC indicado.' },
          { status: 409 }
        )
      )
    );
    const { user } = renderPage();
    await screen.findByText('Aún no hay empresas registradas.');

    await user.click(screen.getByRole('button', { name: 'Nueva empresa' }));
    await user.type(screen.getByLabelText('RUC'), '20123456789');
    await user.type(screen.getByLabelText('Razón social'), 'Boticas SAC');
    await user.click(screen.getByRole('button', { name: 'Crear empresa' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Ya existe una empresa con el RUC indicado.'
    );
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    await user.click(screen.getByRole('button', { name: 'Nueva empresa' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
