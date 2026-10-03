import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleEmpresa } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { EmpresaEditarDialog } from './EmpresaEditarDialog';

const itemUrl = '*/api/v1/organizacion/empresas/empresa-1';

function renderDialog(empresa = sampleEmpresa) {
  const onClose = vi.fn();
  const result = renderRoute(
    '/lista',
    () => <EmpresaEditarDialog empresa={empresa} onClose={onClose} />,
    '/lista'
  );
  return { onClose, ...result };
}

describe('EmpresaEditarDialog', () => {
  it('precarga los datos y deja el RUC de solo lectura', () => {
    renderDialog();

    expect(screen.getByRole('heading', { name: 'Editar empresa' })).toBeInTheDocument();
    expect(screen.getByLabelText('RUC')).toHaveValue('20123456786');
    expect(screen.getByLabelText('RUC')).toHaveAttribute('readonly');
    expect(screen.getByLabelText('Razón social')).toHaveValue('Boticas SAC');
  });

  it('guarda sin enviar el RUC, sin tenant en la consulta, y se cierra', async () => {
    let query = new URLSearchParams();
    let body: Record<string, unknown> = {};
    server.use(
      http.put(itemUrl, async ({ request }) => {
        query = new URL(request.url).searchParams;
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...sampleEmpresa, razonSocial: 'Boticas del Perú SAC' });
      })
    );
    const { onClose, user } = renderDialog();

    const razonSocial = screen.getByLabelText('Razón social');
    await user.clear(razonSocial);
    await user.type(razonSocial, 'Boticas del Perú SAC');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(onClose).toHaveBeenCalledOnce());
    expect(query.has('tenantId')).toBe(false);
    expect(body).toMatchObject({ razonSocial: 'Boticas del Perú SAC', monedaFuncional: 'PEN' });
    expect('ruc' in body).toBe(false);
  });

  it('permite guardar una empresa cuyo RUC guardado no cumple el dígito verificador', async () => {
    server.use(http.put(itemUrl, () => HttpResponse.json(sampleEmpresa)));
    const { onClose, user } = renderDialog({ ...sampleEmpresa, ruc: '20123456789' });

    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(onClose).toHaveBeenCalledOnce());
    expect(
      screen.queryByText('El RUC no es válido: el dígito verificador no coincide.')
    ).toBeNull();
  });

  it('muestra el error del servidor y no se cierra', async () => {
    server.use(
      http.put(itemUrl, () =>
        HttpResponse.json(
          { title: 'Bad Request', detail: 'La razón social no es válida.' },
          { status: 400 }
        )
      )
    );
    const { onClose, user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('La razón social no es válida.');
    expect(onClose).not.toHaveBeenCalled();
  });

  it('llama a onClose desde el botón cerrar', async () => {
    const { onClose, user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(onClose).toHaveBeenCalledOnce();
  });
});
