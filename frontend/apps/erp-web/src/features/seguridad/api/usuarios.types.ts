export type Usuario = {
  id: string;
  tenantId: string;
  documentType: string | null;
  documentNumber: string | null;
  firstNames: string | null;
  lastNames: string | null;
  username: string | null;
  email: string | null;
  displayName: string | null;
  phone: string | null;
  credentialChangeRequired: boolean;
  mfaRequired: boolean;
  status: string;
  createdAt: string;
  updatedAt: string | null;
};

export type CrearUsuarioPayload = {
  tenantId: string;
  documentType?: string | undefined;
  documentNumber?: string | undefined;
  firstNames?: string | undefined;
  lastNames?: string | undefined;
  username?: string | undefined;
  email?: string | undefined;
  phone?: string | undefined;
  displayName?: string | undefined;
  credentialChangeRequired?: boolean | undefined;
  mfaRequired?: boolean | undefined;
};

export type AsignacionRol = {
  id: string;
  roleId: string;
  roleCode: string;
  roleName: string;
  scopeType: string;
  companyId: string | null;
  establishmentId: string | null;
  warehouseId: string | null;
  terminalId: string | null;
  validFrom: string | null;
  validUntil: string | null;
  status: string;
  createdBy: string;
  createdAt: string;
};

export type AsignarRolPayload = {
  tenantId: string;
  roleId: string;
  scopeType: string;
  companyId?: string | undefined;
  establishmentId?: string | undefined;
  warehouseId?: string | undefined;
  terminalId?: string | undefined;
  validFrom?: string | undefined;
  validUntil?: string | undefined;
};
