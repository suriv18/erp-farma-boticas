import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { SKU_VACIO } from '../lib/sku-form';
import { SkuForm, type SkuFormProps } from './SkuForm';

function renderForm(props: Partial<SkuFormProps> = {}) {
  server.use(
    http.get('*/api/v1/catalogo/productos-regulados', () =>
      HttpResponse.json({
        items: [
          {
            id: 'pr-1',
            denominacion: 'Paracetamol 500 mg',
            condicionVentaCodigo: 'VL',
            estadoRegulatorio: 'VIGENTE'
          }
        ],
        page: 0,
        size: 100,
        totalElements: 1
      })
    )
  );
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const onSubmit = vi.fn();
  return {
    onSubmit,
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <SkuForm onSubmit={onSubmit} submitLabel="Guardar" {...props} />
      </QueryClientProvider>
    )
  };
}

describe('SkuForm', () => {
  it('valida los campos obligatorios antes de enviar', async () => {
    const { onSubmit, user } = renderForm();

    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(
      await screen.findByText('El código interno debe tener al menos 2 caracteres.')
    ).toBeInTheDocument();
    expect(screen.getByText('Selecciona la unidad de venta.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('carga las opciones de producto, categoria, marca y unidades y envia los valores', async () => {
    const { onSubmit, user } = renderForm();

    expect(await screen.findByRole('option', { name: 'Paracetamol 500 mg' })).toBeInTheDocument();
    expect(await screen.findByRole('option', { name: 'Bayer' })).toBeInTheDocument();
    expect(await screen.findByRole('option', { name: 'Analgésicos' })).toBeInTheDocument();
    expect((await screen.findAllByRole('option', { name: 'UND — Unidad' })).length).toBeGreaterThan(
      0
    );

    await user.selectOptions(screen.getByLabelText('Producto regulado'), 'pr-1');
    await user.type(screen.getByLabelText('Código interno'), 'SKU-1');
    await user.type(screen.getByLabelText('Descripción comercial'), 'Paracetamol 500 mg x 100');
    await user.selectOptions(screen.getByLabelText('Unidad de venta'), 'UND');
    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(onSubmit).toHaveBeenCalledWith({
      ...SKU_VACIO,
      productoReguladoId: 'pr-1',
      codigoInterno: 'SKU-1',
      descripcionComercial: 'Paracetamol 500 mg x 100',
      unidadVentaCodigo: 'UND'
    });
  });

  it('exige el producto regulado para un SKU regulado y lo ofrece como no regulado', async () => {
    const { onSubmit, user } = renderForm({
      defaultValues: {
        ...SKU_VACIO,
        codigoInterno: 'SKU-2',
        descripcionComercial: 'Alcohol',
        unidadVentaCodigo: 'UND'
      }
    });

    await user.click(screen.getByRole('button', { name: 'Guardar' }));
    expect(
      await screen.findByText('Un SKU regulado requiere un producto regulado asociado.')
    ).toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText('Tipo de SKU'), 'NO_REGULADO');
    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(onSubmit).toHaveBeenCalledTimes(1);
  });

  it('muestra el error del servidor y bloquea el envio', () => {
    renderForm({ error: 'Ya existe un SKU con ese código interno.', isSubmitting: true });

    expect(screen.getByRole('alert')).toHaveTextContent('Ya existe un SKU con ese código interno.');
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled();
  });
});
