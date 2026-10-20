import { screen, waitFor } from '@testing-library/react';
import { server } from '../../../test/mocks/server';
import { miPuestoHandler, sampleMiPuesto } from '../../../test/mi-puesto-fixtures';
import { renderRoute } from '../../../test/render-route';
import { TerminalSelector } from './TerminalSelector';

beforeEach(() => {
  server.use(miPuestoHandler());
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
  it('lista los establecimientos del puesto y, al elegir uno, notifica el cambio sin terminal', async () => {
    const { onChange, user } = renderSelector('', '');

    await screen.findByRole('option', { name: 'Botica Central' });
    await user.selectOptions(screen.getByLabelText('Establecimiento'), 'est-1');

    expect(onChange).toHaveBeenCalledWith('est-1', '');
  });

  it('deshabilita las terminales no operables indicando el motivo', async () => {
    renderSelector('est-2', '');

    const enMantenimiento = await screen.findByRole('option', { name: 'Caja 3 (en mantenimiento)' });

    expect(enMantenimiento).toBeDisabled();
  });

  it('indica el estado en minúsculas cuando el motivo no tiene un texto propio', async () => {
    server.use(
      miPuestoHandler({
        establecimientos: [
          {
            ...sampleMiPuesto.establecimientos[1]!,
            terminales: [
              { id: 'term-9', codigo: 'POS009', nombre: 'Caja 9', estado: 'INACTIVA', operable: false }
            ]
          }
        ]
      })
    );
    renderSelector('est-2', '');

    expect(await screen.findByRole('option', { name: 'Caja 9 (inactiva)' })).toBeDisabled();
  });

  it('muestra las terminales operables y notifica la elegida', async () => {
    const { onChange, user } = renderSelector('est-1', '');

    expect(await screen.findByRole('option', { name: 'Caja 1' })).toBeEnabled();
    await user.selectOptions(screen.getByLabelText('Terminal'), 'term-1');

    await waitFor(() => expect(onChange).toHaveBeenCalledWith('est-1', 'term-1'));
  });

  it('descarta un establecimiento recordado que ya no está en el alcance', async () => {
    const { onChange } = renderSelector('est-x', 'term-x');

    await waitFor(() => expect(onChange).toHaveBeenCalledWith('', ''));
  });

  it('deshabilita el selector de terminal mientras no hay establecimiento', async () => {
    renderSelector('', '');

    expect(await screen.findByLabelText('Terminal')).toBeDisabled();
  });

  it('preselecciona el único establecimiento y su única terminal operable', async () => {
    server.use(
      miPuestoHandler({
        establecimientos: [
          {
            ...sampleMiPuesto.establecimientos[0]!,
            terminales: [sampleMiPuesto.establecimientos[0]!.terminales[0]!]
          }
        ]
      })
    );
    const { onChange } = renderSelector('', '');

    await waitFor(() => expect(onChange).toHaveBeenCalledWith('est-1', 'term-1'));
  });
});
