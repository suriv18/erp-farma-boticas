import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useForm } from 'react-hook-form';
import { CamposFormulario, type CampoFormulario } from './CamposFormulario';

type Valores = {
  nombre: string;
  fecha: string;
  peso: string;
  cantidad: string;
  tipo: string;
  activo: boolean;
};

const campos: CampoFormulario<Valores>[] = [
  { name: 'nombre', label: 'Nombre', tipo: 'texto' },
  { name: 'fecha', label: 'Fecha', tipo: 'fecha' },
  { name: 'peso', label: 'Peso', tipo: 'decimal' },
  { name: 'cantidad', label: 'Cantidad', tipo: 'entero' },
  {
    name: 'tipo',
    label: 'Tipo',
    tipo: 'seleccion',
    opciones: [
      { value: 'A', label: 'Opción A' },
      { value: 'B', label: 'Opción B' }
    ]
  },
  { name: 'activo', label: 'Activo', tipo: 'checkbox' }
];

function Harness({ onSubmit }: { onSubmit: (values: Valores) => void }) {
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors }
  } = useForm<Valores>({
    defaultValues: { nombre: '', fecha: '', peso: '', cantidad: '', tipo: '', activo: false }
  });
  return (
    <form
      onSubmit={(event) => {
        void handleSubmit(onSubmit)(event);
      }}
    >
      <CamposFormulario prefijo="demo" campos={campos} register={register} errors={errors} />
      <button type="button" onClick={() => setError('nombre', { message: 'Nombre inválido.' })}>
        Forzar error
      </button>
      <button type="submit">Enviar</button>
    </form>
  );
}

describe('CamposFormulario', () => {
  it('renderiza cada tipo de campo con su atributo de entrada', () => {
    render(<Harness onSubmit={vi.fn()} />);

    expect(screen.getByLabelText('Nombre')).toHaveAttribute('type', 'text');
    expect(screen.getByLabelText('Nombre')).toHaveAttribute('inputmode', 'text');
    expect(screen.getByLabelText('Fecha')).toHaveAttribute('type', 'date');
    expect(screen.getByLabelText('Peso')).toHaveAttribute('inputmode', 'decimal');
    expect(screen.getByLabelText('Cantidad')).toHaveAttribute('inputmode', 'numeric');
    expect(screen.getByLabelText('Activo')).toHaveAttribute('type', 'checkbox');
    expect(screen.getByRole('option', { name: 'Seleccione…' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Opción B' })).toBeInTheDocument();
  });

  it('registra los valores capturados y muestra errores', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<Harness onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText('Nombre'), 'Paracetamol');
    await user.selectOptions(screen.getByLabelText('Tipo'), 'B');
    await user.click(screen.getByLabelText('Activo'));
    await user.click(screen.getByRole('button', { name: 'Enviar' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ nombre: 'Paracetamol', tipo: 'B', activo: true }),
      expect.anything()
    );

    await user.click(screen.getByRole('button', { name: 'Forzar error' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Nombre inválido.');
  });
});
