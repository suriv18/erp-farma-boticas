export type Permiso = {
  moduleCode: string;
  moduleName: string;
  code: string;
  resource: string;
  action: string;
  name: string;
  description: string | null;
  critical: boolean;
  status: string;
};
