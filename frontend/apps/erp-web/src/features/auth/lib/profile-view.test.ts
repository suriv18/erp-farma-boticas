import { branchLabelOf, initialsOf, roleLabelOf } from './profile-view';

const cajero = {
  roleCode: 'CAJERO',
  roleName: 'Cajero de establecimiento',
  scopeType: 'ESTABLECIMIENTO',
  scopeName: 'Botica Crítica Norte'
};
const admin = { roleCode: 'ADMIN', roleName: 'Administrador global', scopeType: 'GLOBAL', scopeName: null };

describe('initialsOf', () => {
  it('toma la inicial de las dos primeras palabras en mayúscula', () => {
    expect(initialsOf('luis paredes soto')).toBe('LP');
  });

  it('usa una sola inicial cuando el nombre tiene una palabra', () => {
    expect(initialsOf('Luis')).toBe('L');
  });

  it('usa un signo de interrogación cuando no hay nombre', () => {
    expect(initialsOf('   ')).toBe('?');
  });
});

describe('roleLabelOf', () => {
  it('devuelve null sin asignaciones', () => {
    expect(roleLabelOf([])).toBeNull();
  });

  it('une los nombres de rol sin repetir', () => {
    expect(roleLabelOf([cajero, admin, { ...cajero, scopeName: 'Otra botica' }])).toBe(
      'Cajero de establecimiento, Administrador global'
    );
  });
});

describe('branchLabelOf', () => {
  it('devuelve el nombre del ámbito cuando la asignación es de un establecimiento', () => {
    expect(branchLabelOf([admin, cajero])).toBe('Botica Crítica Norte');
  });

  it('indica todas las sucursales cuando el acceso es global', () => {
    expect(branchLabelOf([admin])).toBe('Todas las sucursales');
  });

  it('devuelve null cuando el ámbito no tiene nombre ni es global', () => {
    expect(branchLabelOf([{ ...cajero, scopeName: null }])).toBeNull();
  });

  it('devuelve null sin asignaciones', () => {
    expect(branchLabelOf([])).toBeNull();
  });
});
