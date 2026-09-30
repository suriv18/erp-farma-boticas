import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina, sampleEstablecimiento } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { EstablecimientosSection } from './EstablecimientosSection';

const listUrl = '*/api/v1/organizacion/establecimientos';

function renderSection() {
  return renderRoute(
    '/empresa',
    () => <EstablecimientosSection empresaId="empresa-1" />,
    '/empresa'
  );
}

describe('EstablecimientosSection', () => {
  it('lista los establecimientos de la empresa con enlace al detalle', async () => {
    let received = new URLSearchParams();
    server.use(
      http.get(listUrl, ({ request }) => {
        received = new URL(request.url).searchParams;
        return HttpResponse.json(pagina([sampleEstablecimiento]));
      })
    );

    renderSection();

    const link = await screen.findByRole('link', { name: 'Botica Central' });
    expect(link).toHaveAttribute('href', '/organizacion/establecimientos/est-1');
    expect(screen.getByText('EST001')).toBeInTheDocument();
    expect(screen.getByText('ONLINE')).toBeInTheDocument();
    expect(screen.getByText('ACTIVO')).toBeInTheDocument();
    expect(received.get('empresaId')).toBe('empresa-1');
    expect(received.get('size')).toBe('100');
  });

  it('muestra carga, lista vacía y error de carga', async () => {
    server.use(http.get(listUrl, () => HttpResponse.json(pagina([]))));
    const first = renderSection();
    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(
      await screen.findByText('Esta empresa aún no tiene establecimientos.')
    ).toBeInTheDocument();
    first.unmount();

    server.use(http.get(listUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 })));
    renderSection();
    expect(await screen.findByText('No se pudo cargar los establecimientos.')).toBeInTheDocument();
  });

  it('crea un establecimiento de la empresa y refresca el listado', async () => {
    let created: unknown;
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina(created ? [sampleEstablecimiento] : []))),
      http.post(listUrl, async ({ request }) => {
        created = await request.json();
        return HttpResponse.json(sampleEstablecimiento, { status: 201 });
      })
    );
    const { user } = renderSection();
    await screen.findByText('Esta empresa aún no tiene establecimientos.');

    await user.click(screen.getByRole('button', { name: 'Nuevo establecimiento' }));
    await user.type(screen.getByLabelText('Código'), 'EST001');
    await user.type(screen.getByLabelText('Nombre'), 'Botica Central');
    await user.click(screen.getByRole('button', { name: 'Crear establecimiento' }));

    expect(await screen.findByRole('link', { name: 'Botica Central' })).toBeInTheDocument();
    expect(created).toMatchObject({
      tenantId: 'tenant-1',
      empresaId: 'empresa-1',
      codigo: 'EST001',
      nombre: 'Botica Central',
      perfilOperacion: 'ONLINE'
    });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('muestra el error del servidor al crear y lo limpia al cerrar el modal', async () => {
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina([]))),
      http.post(listUrl, () =>
        HttpResponse.json(
          { title: 'Conflict', detail: 'Ya existe un establecimiento con el código indicado.' },
          { status: 409 }
        )
      )
    );
    const { user } = renderSection();
    await screen.findByText('Esta empresa aún no tiene establecimientos.');

    await user.click(screen.getByRole('button', { name: 'Nuevo establecimiento' }));
    await user.type(screen.getByLabelText('Código'), 'EST001');
    await user.type(screen.getByLabelText('Nombre'), 'Botica Central');
    await user.click(screen.getByRole('button', { name: 'Crear establecimiento' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Ya existe un establecimiento con el código indicado.'
    );
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    await user.click(screen.getByRole('button', { name: 'Nuevo establecimiento' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
