export type PerfilAsignacion = {
  roleCode: string;
  roleName: string;
  scopeType: string;
  scopeName: string | null;
};

export type PerfilUsuario = {
  userId: string;
  tenantId: string;
  displayName: string;
  username: string | null;
  email: string | null;
  assignments: PerfilAsignacion[];
};
