import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina } from '../../../test/organizacion-fixtures';
import { samplePosicion, sampleSku } from '../../../test/inventario-fixtures';
import { AjusteForm } from './AjusteForm';

const almacenes = [
  { id: 'alm-1', nombre: 'Almacén Central', establecimiento: 'Botica Central' },
  { id: 'alm-3', nombre: 'Almacén Norte', establecimiento: 'Botica Norte' }
];

function renderForm(
  posicion: Parameters<typeof AjusteForm>[0]['posicion'],
  error: string | null = null
) {
  const onSubmit = vi.fn();
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <AjusteForm
        posicion={posicion}
        almacenes={almacenes}
        isSubmitting={false}
        error={error}
        onSubmit={onSubmit}
      />
    </QueryClientProvider>
  );
  return { onSubmit, user: userEvent.setup() };
}

beforeEach(() => {
  server.use(http.get('*/api/v1/catalogo/skus', () => HttpResponse.json(pagina([sampleSku]))));
});

describe('AjusteForm', () => {
  it('desde una fila precarga el lote y envía una salida sin pedir datos del lote', async () => {
    const { onSubmit, user } = renderForm(samplePosicion);

    expect(screen.getByText('L001')).toBeInTheDocument();
    expect(screen.queryByLabelText('Almacén')).not.toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Ingreso' })).toHaveValue('AJUSTE_INGRESO');
    expect(screen.getByRole('option', { name: 'Salida' })).toHaveValue('AJUSTE_SALIDA');
    expect(screen.queryByRole('option', { name: 'AJUSTE_SALIDA' })).not.toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText('Tipo de ajuste'), 'AJUSTE_SALIDA');
    await user.type(screen.getByLabelText('Cantidad'), '3');
    await user.type(screen.getByLabelText('Motivo'), 'Merma');
    await user.click(screen.getByRole('button', { name: 'Registrar ajuste' }));

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith({
        almacenId: 'alm-1',
        skuId: 'sku-0001-aaaa',
        tipo: 'AJUSTE_SALIDA',
        loteId: 'lote-1',
        numeroLote: '',
        fechaVencimiento: '',
        cantidad: '3',
        motivo: 'Merma'
      })
    );
  });

  it('sin fila registra un ingreso con lote nuevo', async () => {
    const { onSubmit, user } = renderForm(null);

    await user.selectOptions(screen.getByLabelText('Almacén'), 'alm-3');
    await user.type(screen.getByLabelText('Buscar SKU'), 'para');
    await user.selectOptions(
      screen.getByLabelText('SKU'),
      await screen.findByRole('option', { name: 'MED-001 — Paracetamol 500 mg' })
    );
    await user.type(screen.getByLabelText('Número de lote'), 'L-NEW');
    await user.type(screen.getByLabelText('Fecha de vencimiento'), '2030-01-01');
    await user.type(screen.getByLabelText('Cantidad'), '12');
    await user.type(screen.getByLabelText('Motivo'), 'Ingreso inicial');
    await user.click(screen.getByRole('button', { name: 'Registrar ingreso' }));

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith({
        almacenId: 'alm-3',
        skuId: 'sku-0001-aaaa',
        tipo: 'AJUSTE_INGRESO',
        loteId: '',
        numeroLote: 'L-NEW',
        fechaVencimiento: '2030-01-01',
        cantidad: '12',
        motivo: 'Ingreso inicial'
      })
    );
  });

  it('muestra los errores de validación sin enviar', async () => {
    const { onSubmit, user } = renderForm(null);

    await user.click(screen.getByRole('button', { name: 'Registrar ingreso' }));

    expect(await screen.findByText('Selecciona un almacén.')).toBeInTheDocument();
    expect(screen.getByText('Selecciona un SKU.')).toBeInTheDocument();
    expect(screen.getByText('Indica el número del lote.')).toBeInTheDocument();
    expect(screen.getByText('Indica la fecha de vencimiento.')).toBeInTheDocument();
    expect(screen.getByText('El motivo es obligatorio.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('muestra el error recibido del servidor', () => {
    renderForm(samplePosicion, 'Stock insuficiente.');

    expect(screen.getByRole('alert')).toHaveTextContent('Stock insuficiente.');
  });
});
