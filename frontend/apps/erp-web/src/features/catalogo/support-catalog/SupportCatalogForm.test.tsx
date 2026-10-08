import { zodResolver } from '@hookform/resolvers/zod';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { z } from 'zod';
import { SupportCatalogForm } from './SupportCatalogForm';
import type { FieldDef } from './support-catalog.types';

const fields: FieldDef[] = [
  { name: 'codigo', label: 'Código', type: 'text' },
  { name: 'denominacion', label: 'Denominación', type: 'text' },
  { name: 'notas', label: 'Notas', type: 'textarea' },
  { name: 'orden', label: 'Orden', type: 'number' },
  { name: 'activo', label: 'Activo', type: 'checkbox' }
];

const schema = z.object({
  codigo: z.string().min(1, 'Requerido'),
  denominacion: z.string().min(1, 'Requerido'),
  notas: z.string().optional(),
  orden: z.coerce.number().optional(),
  activo: z.boolean().optional()
});

const resolver = zodResolver(schema);

describe('SupportCatalogForm', () => {
  it('renderiza un input por cada FieldDef segun su type', () => {
    render(
      <SupportCatalogForm
        fields={fields}
        resolver={resolver}
        onSubmit={() => {}}
        submitLabel="Guardar"
      />
    );

    expect(screen.getByLabelText('Código')).toHaveAttribute('type', 'text');
    expect(screen.getByLabelText('Denominación')).toHaveAttribute('type', 'text');
    expect(screen.getByLabelText('Notas').tagName).toBe('TEXTAREA');
    expect(screen.getByLabelText('Orden')).toHaveAttribute('type', 'number');
    expect(screen.getByLabelText('Activo')).toHaveAttribute('type', 'checkbox');
  });

  it('muestra errores de validacion y no llama onSubmit si el schema falla', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(
      <SupportCatalogForm
        fields={fields}
        resolver={resolver}
        onSubmit={onSubmit}
        submitLabel="Guardar"
      />
    );

    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(await screen.findAllByText('Requerido')).toHaveLength(2);
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('llama onSubmit con los valores del formulario cuando es valido', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(
      <SupportCatalogForm
        fields={fields}
        resolver={resolver}
        onSubmit={onSubmit}
        submitLabel="Guardar"
      />
    );

    await user.type(screen.getByLabelText('Código'), 'ORAL');
    await user.type(screen.getByLabelText('Denominación'), 'Vía oral');
    await user.type(screen.getByLabelText('Orden'), '3');
    await user.click(screen.getByLabelText('Activo'));
    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ codigo: 'ORAL', denominacion: 'Vía oral', orden: 3, activo: true })
    );
  });
});
