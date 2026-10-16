import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina, sampleTerminal } from '../../../test/organizacion-fixtures';
import { sampleEstructura } from '../../../test/inventario-fixtures';
import { renderRoute } from '../../../test/render-route';
import { TerminalSelector } from './TerminalSelector';

beforeEach(() => {
  server.use(
    http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleEstructura)),
    http.get('*/api/v1/organizacion/terminales-pos', ({ request }) => {
      const establecimiento = new URL(request.url).searchParams.get('establecimientoId');
      return HttpResponse.json(
        pagina(
          establecimiento === 'est-1'
            ? [
                { ...sampleTerminal, id: 'term-1', nombre: 'Caja 1', estado: 'ACTIVO' },
                { ...sampleTerminal, id: 'term-2', nombre: 'Caja 2', estado: 'BLOQUEADO' }
              ]
            : []
        )
      );
    })
  );
});

function renderSelector(establecimientoId: string, terminalId: string, onChange = vi.fn()) {
  const Selector = () => (
    <TerminalSelector
      establecimientoId={establecimientoId}
      terminalId={terminalId}
      onChange={onChange}
    />
  );
  return { onChange, ...renderRoute('/x', Selector, '/x') };
}

describe('TerminalSelector', () => {
  it('lista los establecimientos y, al elegir uno, notifica el cambio sin terminal', async () => {
    const { onChange, user } = renderSelector('', '');

    await screen.findByRole('option', { name: 'Botica Central' });
    await user.selectOptions(screen.getByLabelText('Establecimiento'), 'est-1');

    expect(onChange).toHaveBeenCalledWith('est-1', '');
  });

  it('muestra solo las terminales activas del establecimiento y notifica la elegida', async () => {
    const { onChange, user } = renderSelector('est-1', '');

    expect(await screen.findByRole('option', { name: 'Caja 1' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Caja 2' })).not.toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText('Terminal'), 'term-1');

    await waitFor(() => expect(onChange).toHaveBeenCalledWith('est-1', 'term-1'));
  });

  it('deshabilita el selector de terminal mientras no hay establecimiento', async () => {
    renderSelector('', '');

    expect(await screen.findByLabelText('Terminal')).toBeDisabled();
  });
});
