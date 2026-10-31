export const tienePermiso = (permisos: readonly string[], requerido: string): boolean =>
  requerido.endsWith('.')
    ? permisos.some((permiso) => permiso.startsWith(requerido))
    : permisos.includes(requerido);
