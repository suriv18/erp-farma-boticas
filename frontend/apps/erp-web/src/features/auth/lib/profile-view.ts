import type { PerfilAsignacion } from '../api/profile.types';

const GLOBAL_SCOPE = 'GLOBAL';

export function initialsOf(name: string): string {
  const letters = name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((word) => word.charAt(0).toUpperCase());
  return letters.length > 0 ? letters.join('') : '?';
}

export function roleLabelOf(assignments: PerfilAsignacion[]): string | null {
  const names = [...new Set(assignments.map((assignment) => assignment.roleName))];
  return names.length > 0 ? names.join(', ') : null;
}

export function branchLabelOf(assignments: PerfilAsignacion[]): string | null {
  const scoped = assignments.find(
    (assignment) => assignment.scopeType !== GLOBAL_SCOPE && assignment.scopeName
  );
  if (scoped) return scoped.scopeName;
  return assignments.some((assignment) => assignment.scopeType === GLOBAL_SCOPE)
    ? 'Todas las sucursales'
    : null;
}
