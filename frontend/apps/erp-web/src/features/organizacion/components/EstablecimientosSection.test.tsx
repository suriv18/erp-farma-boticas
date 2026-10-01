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
  it('lista los establecimientos de la empresa con acciones', async () => {
    let received = new URLSearchParams();
    server.use(
      http.get(listUrl, ({ request }) => {
        received = new URL(request.url).searchParams;
        return HttpResponse.json(pagina([sampleEstablecimiento]));
      })
    );

    renderSection();

    expect(await screen.findByText('Botica Central')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Botica Central' })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver detalle de Botica Central' })).toHaveAttribute(
      'href',
      '/organizacion/establecimientos/est-1'
    );
    expect(screen.getByRole('button', { name: 'Editar Botica Central' })).toBeInTheDocument();
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

    expect(await screen.findByText('Botica Central')).toBeInTheDocument();
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

  it('edita un establecimiento desde la lista sin salir de la página', async () => {
    let nombre = 'Botica Central';
    let body: Record<string, unknown> = {};
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina([{ ...sampleEstablecimiento, nombre }]))),
      http.put(`${listUrl}/est-1`, async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        nombre = String(body.nombre);
        return HttpResponse.json({ ...sampleEstablecimiento, nombre });
      })
    );
    const { user, router } = renderSection();
    await screen.findByText('Botica Central');

    await user.click(screen.getByRole('button', { name: 'Editar Botica Central' }));
    expect(screen.getByRole('heading', { name: 'Editar establecimiento' })).toBeInTheDocument();
    expect(screen.getByLabelText('Código', { exact: true })).toHaveAttribute('readonly');
    const campoNombre = screen.getByLabelText('Nombre', { exact: true });
    await user.clear(campoNombre);
    await user.type(campoNombre, 'Botica Principal');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByText('Botica Principal')).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(body).toMatchObject({ nombre: 'Botica Principal' });
    expect(router.state.location.pathname).toBe('/empresa');
  });

  it('cierra el modal de edición con Cerrar sin guardar', async () => {
    server.use(http.get(listUrl, () => HttpResponse.json(pagina([sampleEstablecimiento]))));
    const { user } = renderSection();
    await screen.findByText('Botica Central');

    await user.click(screen.getByRole('button', { name: 'Editar Botica Central' }));
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('deshabilita el alta y explica el motivo cuando la empresa no admite altas', async () => {
    server.use(http.get(listUrl, () => HttpResponse.json(pagina([]))));
    renderRoute(
      '/empresa',
      () => (
        <EstablecimientosSection
          empresaId="empresa-1"
          motivoSinAltas="La empresa está suspendida; no admite establecimientos nuevos."
        />
      ),
      '/empresa'
    );
    await screen.findByText('Esta empresa aún no tiene establecimientos.');

    const boton = screen.getByRole('button', { name: 'Nuevo establecimiento' });
    expect(boton).toBeDisabled();
    expect(boton).toHaveAttribute(
      'title',
      'La empresa está suspendida; no admite establecimientos nuevos.'
    );
  });
});
