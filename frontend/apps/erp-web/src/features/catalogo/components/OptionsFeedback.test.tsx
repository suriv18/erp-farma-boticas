import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { OptionsFeedback } from './OptionsFeedback';

it('distingue carga, error recuperable y opciones listas', async () => {
  const retry = vi.fn();
  const { rerender } = render(
    <OptionsFeedback queries={[{ isPending: true, isError: false, refetch: retry }]} />
  );
  expect(screen.getByRole('status')).toHaveTextContent('Cargando opciones');
  rerender(<OptionsFeedback queries={[{ isPending: false, isError: true, refetch: retry }]} />);
  await userEvent.click(screen.getByRole('button', { name: 'Reintentar opciones' }));
  expect(retry).toHaveBeenCalledOnce();
  rerender(<OptionsFeedback queries={[{ isPending: false, isError: false, refetch: retry }]} />);
  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  expect(screen.queryByRole('status')).not.toBeInTheDocument();
});
