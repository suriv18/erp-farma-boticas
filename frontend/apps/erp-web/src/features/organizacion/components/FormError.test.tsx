import { render, screen } from '@testing-library/react';
import { FormError } from './FormError';

describe('FormError', () => {
  it('muestra el mensaje como alerta', () => {
    render(<FormError message="Ya existe una empresa con el RUC indicado." />);

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Ya existe una empresa con el RUC indicado.'
    );
  });
});
