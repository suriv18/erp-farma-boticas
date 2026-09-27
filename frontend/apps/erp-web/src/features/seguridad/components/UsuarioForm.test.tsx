import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UsuarioForm } from './UsuarioForm';

function renderForm(props: Parameters<typeof UsuarioForm>[0]) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <UsuarioForm {...props} />
      </QueryClientProvider>
    )
  };
}

describe('UsuarioForm', () => {
  it('muestra un error cuando el correo esta vacio', async () => {
    const onSubmit = vi.fn();
    const { user } = renderForm({ onSubmit, submitLabel: 'Crear usuario' });

    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByText('Ingresa un correo electrónico válido.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('muestra un error cuando el correo es invalido', async () => {
    const onSubmit = vi.fn();
    const { user } = renderForm({ onSubmit, submitLabel: 'Crear usuario' });

    await user.type(screen.getByLabelText('Correo'), 'no-es-un-correo');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByText('Ingresa un correo electrónico válido.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('envia los valores cuando el formulario es valido', async () => {
    const onSubmit = vi.fn();
    const { user } = renderForm({ onSubmit, submitLabel: 'Crear usuario' });

    await screen.findByRole('option', { name: 'DNI' });
    await user.type(screen.getByLabelText('Número de documento'), '45678912');
    await user.type(screen.getByLabelText('Correo'), 'ada@boticas.pe');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        email: 'ada@boticas.pe',
        documentType: '1',
        documentNumber: '45678912'
      })
    );
  });

  it('carga las opciones de tipo de documento desde el catalogo', async () => {
    const onSubmit = vi.fn();
    renderForm({ onSubmit, submitLabel: 'Crear usuario' });

    expect(await screen.findByRole('option', { name: 'DNI' })).toBeInTheDocument();
  });

  it('preselecciona DNI por defecto al crear un usuario', async () => {
    const onSubmit = vi.fn();
    renderForm({ onSubmit, submitLabel: 'Crear usuario' });

    await screen.findByRole('option', { name: 'DNI' });
    expect(screen.getByLabelText('Tipo de documento')).toHaveValue('1');
  });

  it('exige el numero de documento al crear un usuario', async () => {
    const onSubmit = vi.fn();
    const { user } = renderForm({ onSubmit, submitLabel: 'Crear usuario' });

    await screen.findByRole('option', { name: 'DNI' });
    await user.type(screen.getByLabelText('Correo'), 'ada@boticas.pe');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByText('Ingresa el número de documento.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('permite elegir otro tipo de documento distinto al preseleccionado', async () => {
    const onSubmit = vi.fn();
    const { user } = renderForm({ onSubmit, submitLabel: 'Crear usuario' });

    await screen.findByRole('option', { name: 'CE' });
    await user.selectOptions(screen.getByLabelText('Tipo de documento'), '4');
    await user.type(screen.getByLabelText('Número de documento'), 'X1234567');
    await user.type(screen.getByLabelText('Correo'), 'ada@boticas.pe');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ documentType: '4', documentNumber: 'X1234567' })
    );
  });
});
