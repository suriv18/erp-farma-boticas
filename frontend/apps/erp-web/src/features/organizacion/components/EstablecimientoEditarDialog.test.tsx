import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleEstablecimiento } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { EstablecimientoEditarDialog } from './EstablecimientoEditarDialog';

const itemUrl = '*/api/v1/organizacion/establecimientos/est-1';

function renderDialog() {
  const onClose = vi.fn();
  const result = renderRoute(
    '/lista',
    () => <EstablecimientoEditarDialog establecimiento={sampleEstablecimiento} onClose={onClose} />,
    '/lista'
  );
  return { onClose, ...result };
}

describe('EstablecimientoEditarDialog', () => {
  it('precarga los datos y deja el código de solo lectura', () => {
    renderDialog();

    expect(screen.getByRole('heading', { name: 'Editar establecimiento' })).toBeInTheDocument();
    expect(screen.getByLabelText('Código', { exact: true })).toHaveValue('EST001');
    expect(screen.getByLabelText('Código', { exact: true })).toHaveAttribute('readonly');
    expect(screen.getByLabelText('Nombre', { exact: true })).toHaveValue('Botica Central');
  });

  it('guarda sin enviar el código, con el tenant en la consulta, y se cierra', async () => {
    let query = new URLSearchParams();
    let body: Record<string, unknown> = {};
    server.use(
      http.put(itemUrl, async ({ request }) => {
        query = new URL(request.url).searchParams;
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...sampleEstablecimiento, nombre: 'Botica Principal' });
      })
    );
    const { onClose, user } = renderDialog();

    const nombre = screen.getByLabelText('Nombre', { exact: true });
    await user.clear(nombre);
    await user.type(nombre, 'Botica Principal');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(onClose).toHaveBeenCalledOnce());
    expect(query.get('tenantId')).toBe('tenant-1');
    expect(body).toMatchObject({ nombre: 'Botica Principal', perfilOperacion: 'ONLINE' });
    expect('codigo' in body).toBe(false);
  });

  it('muestra el error del servidor y no se cierra', async () => {
    server.use(
      http.put(itemUrl, () =>
        HttpResponse.json(
          { title: 'Bad Request', detail: 'El nombre no es válido.' },
          { status: 400 }
        )
      )
    );
    const { onClose, user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('El nombre no es válido.');
    expect(onClose).not.toHaveBeenCalled();
  });

  it('llama a onClose desde el botón cerrar', async () => {
    const { onClose, user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(onClose).toHaveBeenCalledOnce();
  });
});
