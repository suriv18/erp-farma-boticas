import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CheckboxField, SelectField, TextField } from './FormFields';

describe('TextField', () => {
  it('asocia la etiqueta con el input y no marca error', () => {
    render(<TextField id="campo" label="Nombre" />);

    const input = screen.getByLabelText('Nombre');
    expect(input).toHaveAttribute('id', 'campo');
    expect(input).not.toHaveAttribute('aria-invalid');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('muestra el error y marca el input como inválido', () => {
    render(<TextField id="campo" label="Nombre" error="Es obligatorio." />);

    expect(screen.getByRole('alert')).toHaveTextContent('Es obligatorio.');
    expect(screen.getByLabelText('Nombre')).toHaveAttribute('aria-invalid', 'true');
  });

  it('reenvía props del input como readOnly y placeholder', () => {
    render(<TextField id="campo" label="Nombre" readOnly placeholder="Escribe" />);

    const input = screen.getByLabelText('Nombre');
    expect(input).toHaveAttribute('readonly');
    expect(input).toHaveAttribute('placeholder', 'Escribe');
  });
});

describe('SelectField', () => {
  it('renderiza las opciones y permite elegir', async () => {
    const user = userEvent.setup();
    render(
      <SelectField id="tipo" label="Tipo" defaultValue="a">
        <option value="a">A</option>
        <option value="b">B</option>
      </SelectField>
    );

    const select = screen.getByLabelText('Tipo');
    expect(select).toHaveValue('a');
    await user.selectOptions(select, 'b');
    expect(select).toHaveValue('b');
    expect(select).not.toHaveAttribute('aria-invalid');
  });

  it('muestra el error y marca el select como inválido', () => {
    render(
      <SelectField id="tipo" label="Tipo" error="Selecciona un tipo.">
        <option value="a">A</option>
      </SelectField>
    );

    expect(screen.getByRole('alert')).toHaveTextContent('Selecciona un tipo.');
    expect(screen.getByLabelText('Tipo')).toHaveAttribute('aria-invalid', 'true');
  });
});

describe('CheckboxField', () => {
  it('asocia la etiqueta y alterna el valor', async () => {
    const user = userEvent.setup();
    render(<CheckboxField id="principal" label="Es principal" />);

    const checkbox = screen.getByLabelText('Es principal');
    expect(checkbox).not.toBeChecked();
    await user.click(checkbox);
    expect(checkbox).toBeChecked();
  });

  it('respeta defaultChecked', () => {
    render(<CheckboxField id="activo" label="Activo" defaultChecked />);

    expect(screen.getByLabelText('Activo')).toBeChecked();
  });

  it('muestra el error debajo de la casilla', () => {
    render(<CheckboxField id="activo" label="Activo" error="Es obligatorio." />);

    expect(screen.getByRole('alert')).toHaveTextContent('Es obligatorio.');
  });

  it('no muestra error cuando no se recibe', () => {
    render(<CheckboxField id="activo" label="Activo" />);

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
