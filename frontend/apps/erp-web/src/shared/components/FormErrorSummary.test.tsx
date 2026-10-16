import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { FormErrorSummary } from './FormErrorSummary';

it('omite errores sin mensaje y permite enfocar el campo desde el resumen', async () => {
  const { rerender } = render(<FormErrorSummary prefix="form" errors={{}} />);
  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  rerender(
    <>
      <input id="form-nombre" aria-label="Nombre" />
      <FormErrorSummary
        prefix="form"
        errors={{
          nombre: { type: 'required', message: 'Ingresa un nombre' },
          otro: { type: 'required' }
        }}
      />
    </>
  );
  await userEvent.click(screen.getByRole('link', { name: 'Ingresa un nombre' }));
  expect(screen.getByLabelText('Nombre')).toHaveFocus();
});
