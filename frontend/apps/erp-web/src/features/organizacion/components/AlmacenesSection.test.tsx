import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina, sampleAlmacen } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { AlmacenesSection } from './AlmacenesSection';

const listUrl = '*/api/v1/organizacion/almacenes';
const itemUrl = '*/api/v1/organizacion/almacenes/alm-1';

function renderSection() {
  return renderRoute(
    '/establecimiento',
    () => <AlmacenesSection establecimientoId="est-1" />,
    '/establecimiento'
  );
}

describe('AlmacenesSection', () => {
  it('lista los almacenes del establecimiento con su estado', async () => {
    let received = new URLSearchParams();
    server.use(
      http.get(listUrl, ({ request }) => {
        received = new URL(request.url).searchParams;
        return HttpResponse.json(
          pagina([
            sampleAlmacen,
            {
              ...sampleAlmacen,
              id: 'alm-2',
              codigo: 'ALM002',
              nombre: 'Almacén Norte',
              tipo: 'CUARENTENA',
              activo: false
            }
          ])
        );
      })
    );

    renderSection();

    expect(await screen.findByText('Almacén Central')).toBeInTheDocument();
    expect(screen.getByText('ALM001')).toBeInTheDocument();
    expect(screen.getByText('GENERAL')).toBeInTheDocument();
    expect(screen.getByText('CUARENTENA')).toBeInTheDocument();
    expect(screen.getByText('ACTIVO')).toBeInTheDocument();
    expect(screen.getByText('INACTIVO')).toBeInTheDocument();
    expect(received.get('establecimientoId')).toBe('est-1');
    expect(received.get('size')).toBe('100');
  });

  it('muestra carga, lista vacía y error de carga', async () => {
    server.use(http.get(listUrl, () => HttpResponse.json(pagina([]))));
    const first = renderSection();
    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(
      await screen.findByText('Este establecimiento aún no tiene almacenes.')
    ).toBeInTheDocument();
    first.unmount();

    server.use(http.get(listUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 })));
    renderSection();
    expect(await screen.findByText('No se pudo cargar los almacenes.')).toBeInTheDocument();
  });

  it('crea un almacén del establecimiento y refresca el listado', async () => {
    let created: unknown;
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina(created ? [sampleAlmacen] : []))),
      http.post(listUrl, async ({ request }) => {
        created = await request.json();
        return HttpResponse.json(sampleAlmacen, { status: 201 });
      })
    );
    const { user } = renderSection();
    await screen.findByText('Este establecimiento aún no tiene almacenes.');

    await user.click(screen.getByRole('button', { name: 'Nuevo almacén' }));
    await user.type(screen.getByLabelText('Código'), 'ALM001');
    await user.type(screen.getByLabelText('Nombre'), 'Almacén Central');
    await user.click(screen.getByRole('button', { name: 'Crear almacén' }));

    expect(await screen.findByText('Almacén Central')).toBeInTheDocument();
    expect(created).toMatchObject({
      tenantId: 'tenant-1',
      establecimientoId: 'est-1',
      codigo: 'ALM001',
      nombre: 'Almacén Central',
      tipo: 'GENERAL'
    });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('muestra el error del servidor al crear y lo limpia al cerrar el modal', async () => {
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina([]))),
      http.post(listUrl, () =>
        HttpResponse.json(
          { title: 'Conflict', detail: 'Ya existe un almacén con el código indicado.' },
          { status: 409 }
        )
      )
    );
    const { user } = renderSection();
    await screen.findByText('Este establecimiento aún no tiene almacenes.');

    await user.click(screen.getByRole('button', { name: 'Nuevo almacén' }));
    await user.type(screen.getByLabelText('Código'), 'ALM001');
    await user.type(screen.getByLabelText('Nombre'), 'Almacén Central');
    await user.click(screen.getByRole('button', { name: 'Crear almacén' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Ya existe un almacén con el código indicado.'
    );
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    await user.click(screen.getByRole('button', { name: 'Nuevo almacén' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('edita un almacén precargando sus datos y permite desactivarlo', async () => {
    let query = new URLSearchParams();
    let body: Record<string, unknown> = {};
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina([sampleAlmacen]))),
      http.put(itemUrl, async ({ request }) => {
        query = new URL(request.url).searchParams;
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...sampleAlmacen, activo: false });
      })
    );
    const { user } = renderSection();
    await screen.findByText('Almacén Central');

    await user.click(screen.getByRole('button', { name: 'Editar Almacén Central' }));
    const codigo = screen.getByLabelText('Código');
    expect(codigo).toHaveValue('ALM001');
    expect(codigo).toHaveAttribute('readonly');
    const nombre = screen.getByLabelText('Nombre');
    await user.clear(nombre);
    await user.type(nombre, 'Almacén Principal');
    await user.click(screen.getByLabelText('Almacén activo'));
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(query.get('tenantId')).toBe('tenant-1');
    expect(body).toMatchObject({ nombre: 'Almacén Principal', tipo: 'GENERAL', activo: false });
    expect('codigo' in body).toBe(false);
  });

  it('muestra el error del servidor al editar y lo limpia al cerrar', async () => {
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina([sampleAlmacen]))),
      http.put(itemUrl, () =>
        HttpResponse.json(
          { title: 'Bad Request', detail: 'El nombre no es válido.' },
          { status: 400 }
        )
      )
    );
    const { user } = renderSection();
    await screen.findByText('Almacén Central');

    await user.click(screen.getByRole('button', { name: 'Editar Almacén Central' }));
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('El nombre no es válido.');
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    await user.click(screen.getByRole('button', { name: 'Editar Almacén Central' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('deshabilita el alta y explica el motivo cuando el establecimiento no admite altas', async () => {
    server.use(http.get(listUrl, () => HttpResponse.json(pagina([]))));
    renderRoute(
      '/establecimiento',
      () => (
        <AlmacenesSection
          establecimientoId="est-1"
          motivoSinAltas="El establecimiento está clausurado; no admite almacenes ni terminales POS nuevos."
        />
      ),
      '/establecimiento'
    );
    await screen.findByText('Este establecimiento aún no tiene almacenes.');

    const boton = screen.getByRole('button', { name: 'Nuevo almacén' });
    expect(boton).toBeDisabled();
    expect(boton).toHaveAttribute(
      'title',
      'El establecimiento está clausurado; no admite almacenes ni terminales POS nuevos.'
    );
  });
});
