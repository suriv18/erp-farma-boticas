import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useForm } from 'react-hook-form';
import { describe, expect, it, vi } from 'vitest';
import { CamposTexto, type CampoTexto } from './CamposTexto';

type Valores = { codigo: string; monto: string };

const CAMPOS: ReadonlyArray<CampoTexto<Valores>> = [
  { name: 'codigo', id: 'f-codigo', label: 'Código', readOnly: true },
  { name: 'monto', id: 'f-monto', label: 'Monto', inputMode: 'decimal' }
];

function Prueba({ onSubmit }: { onSubmit: (v: Valores) => void }) {
  const {
    register,
    handleSubmit,
    formState: { errors }
  } = useForm<Valores>({
    defaultValues: { codigo: 'C-1', monto: '' },
    resolver: (values) =>
      values.monto
        ? { values, errors: {} }
        : { values: {}, errors: { monto: { type: 'required', message: 'Monto requerido' } } }
  });

  return (
    <form
      noValidate
      onSubmit={(event) => {
        void handleSubmit(onSubmit)(event);
      }}
    >
      <CamposTexto fields={CAMPOS} register={register} errors={errors} />
      <button type="submit">Enviar</button>
    </form>
  );
}

describe('CamposTexto', () => {
  it('renderiza cada descriptor con etiqueta, valor y atributos', () => {
    render(<Prueba onSubmit={vi.fn()} />);

    const codigo = screen.getByLabelText('Código');
    const monto = screen.getByLabelText('Monto');

    expect(codigo).toHaveValue('C-1');
    expect(codigo).toHaveAttribute('readonly');
    expect(codigo).not.toHaveAttribute('inputmode');
    expect(monto).not.toHaveAttribute('readonly');
    expect(monto).toHaveAttribute('inputmode', 'decimal');
    expect(monto).not.toHaveAttribute('aria-invalid');
  });

  it('muestra el error del campo con aria-invalid y no envía', async () => {
    const onSubmit = vi.fn();
    render(<Prueba onSubmit={onSubmit} />);

    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }));

    expect(await screen.findByText('Monto requerido')).toBeInTheDocument();
    expect(screen.getByLabelText('Monto')).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByLabelText('Código')).not.toHaveAttribute('aria-invalid');
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('registra los valores escritos y los envía', async () => {
    const onSubmit = vi.fn();
    render(<Prueba onSubmit={onSubmit} />);

    await userEvent.type(screen.getByLabelText('Monto'), '12.5');
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }));

    await vi.waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
    expect(onSubmit.mock.calls[0]?.[0]).toEqual({ codigo: 'C-1', monto: '12.5' });
    expect(screen.queryByText('Monto requerido')).not.toBeInTheDocument();
  });
});
