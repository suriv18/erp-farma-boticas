import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleEstructura } from '../../../test/inventario-fixtures';
import { pagina, sampleTerminal } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import type { PuestoTrabajo } from '../../organizacion';
import { PuestoVenta } from './PuestoVenta';

const puestoBase: PuestoTrabajo = {
  establecimientoId: 'est-1',
  terminalId: 'term-1',
  almacenId: 'alm-1'
};

beforeEach(() => {
  server.use(
    http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleEstructura)),
    http.get('*/api/v1/organizacion/terminales-pos', () =>
      HttpResponse.json(
        pagina([
          { ...sampleTerminal, id: 'term-1', nombre: 'Caja 1', estado: 'ACTIVO' },
          { ...sampleTerminal, id: 'term-2', nombre: 'Caja 2', estado: 'ACTIVO' }
        ])
      )
    )
  );
});

function renderPuesto(puesto: PuestoTrabajo) {
  const onChange = vi.fn();
  const Pantalla = () => <PuestoVenta puesto={puesto} onChange={onChange} />;
  return { onChange, ...renderRoute('/pos', Pantalla, '/pos') };
}

describe('PuestoVenta', () => {
  it('muestra el selector de terminal y los almacenes del establecimiento', async () => {
    renderPuesto(puestoBase);

    expect(await screen.findByLabelText('Terminal')).toBeInTheDocument();
    expect(await screen.findByRole('option', { name: 'Almacén Central' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Almacén Frío' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Selecciona un almacén' })).toBeInTheDocument();
  });

  it('notifica el almacén elegido conservando el resto del puesto', async () => {
    const { onChange, user } = renderPuesto({ ...puestoBase, almacenId: '' });

    await screen.findByRole('option', { name: 'Almacén Central' });
    await user.selectOptions(screen.getByLabelText('Almacén'), 'alm-1');

    expect(onChange).toHaveBeenCalledWith({ ...puestoBase, almacenId: 'alm-1' });
  });

  it('limpia terminal y almacén al elegir otro establecimiento', async () => {
    const { onChange, user } = renderPuesto(puestoBase);

    await screen.findByRole('option', { name: 'Botica Norte' });
    await user.selectOptions(screen.getByLabelText('Establecimiento'), 'est-2');

    expect(onChange).toHaveBeenCalledWith({
      establecimientoId: 'est-2',
      terminalId: '',
      almacenId: ''
    });
  });

  it('conserva el almacén al elegir otra terminal del mismo establecimiento', async () => {
    const { onChange, user } = renderPuesto(puestoBase);

    await screen.findByRole('option', { name: 'Caja 2' });
    await user.selectOptions(screen.getByLabelText('Terminal'), 'term-2');

    await waitFor(() =>
      expect(onChange).toHaveBeenCalledWith({ ...puestoBase, terminalId: 'term-2' })
    );
  });

  it('deshabilita el selector de almacén sin establecimiento', async () => {
    renderPuesto({ establecimientoId: '', terminalId: '', almacenId: '' });

    expect(await screen.findByLabelText('Almacén')).toBeDisabled();
  });
});
