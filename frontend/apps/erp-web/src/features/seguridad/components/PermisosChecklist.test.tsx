import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { Permiso } from '../api/permisos.types';
import { PermisosChecklist } from './PermisosChecklist';

const permisos: Permiso[] = [
  {
    moduleCode: 'SEGURIDAD',
    moduleName: 'Seguridad',
    code: 'seguridad.usuarios.consultar',
    resource: 'USUARIO',
    action: 'CONSULTAR',
    name: 'Consultar usuarios',
    description: null,
    critical: false,
    status: 'ACTIVO'
  },
  {
    moduleCode: 'CATALOGO',
    moduleName: 'Catálogo',
    code: 'catalogo.skus.consultar',
    resource: 'SKU',
    action: 'CONSULTAR',
    name: 'Consultar SKU',
    description: null,
    critical: false,
    status: 'ACTIVO'
  }
];

describe('PermisosChecklist', () => {
  it('agrupa los permisos por modulo', () => {
    render(<PermisosChecklist permisos={permisos} selectedCodes={new Set()} onChange={() => {}} />);
    expect(screen.getByText('Seguridad')).toBeInTheDocument();
    expect(screen.getByText('Catálogo')).toBeInTheDocument();
  });

  it('marca los checkboxes de los codigos seleccionados', () => {
    render(
      <PermisosChecklist
        permisos={permisos}
        selectedCodes={new Set(['seguridad.usuarios.consultar'])}
        onChange={() => {}}
      />
    );
    expect(screen.getByLabelText('Consultar usuarios')).toBeChecked();
    expect(screen.getByLabelText('Consultar SKU')).not.toBeChecked();
  });

  it('llama a onChange con el nuevo set al marcar un permiso', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<PermisosChecklist permisos={permisos} selectedCodes={new Set()} onChange={onChange} />);

    await user.click(screen.getByLabelText('Consultar usuarios'));

    expect(onChange).toHaveBeenCalledWith(new Set(['seguridad.usuarios.consultar']));
  });

  it('llama a onChange con el codigo removido al desmarcar un permiso', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(
      <PermisosChecklist
        permisos={permisos}
        selectedCodes={new Set(['seguridad.usuarios.consultar'])}
        onChange={onChange}
      />
    );

    await user.click(screen.getByLabelText('Consultar usuarios'));

    expect(onChange).toHaveBeenCalledWith(new Set());
  });
});
