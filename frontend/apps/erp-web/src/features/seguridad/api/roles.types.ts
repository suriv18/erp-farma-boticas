export type Rol = {
  id: string;
  tenantId: string;
  code: string;
  name: string;
  description: string | null;
  roleType: string;
  systemRole: boolean;
  permissionCodes: string[];
  status: string;
  createdAt: string;
  updatedAt: string | null;
};

export type PaginaResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
};

export type CrearRolPayload = {
  tenantId: string;
  code: string;
  name: string;
  description?: string | undefined;
  roleType: string;
  systemRole?: boolean | undefined;
};
