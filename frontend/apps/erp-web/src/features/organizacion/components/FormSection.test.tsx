import { render, screen } from '@testing-library/react';
import { FormSection } from './FormSection';

describe('FormSection', () => {
  it('agrupa su contenido bajo un título accesible', () => {
    render(
      <FormSection title="Regulatorio">
        <p>Contenido</p>
      </FormSection>
    );

    const group = screen.getByRole('group', { name: 'Regulatorio' });
    expect(group).toHaveTextContent('Contenido');
  });
});
