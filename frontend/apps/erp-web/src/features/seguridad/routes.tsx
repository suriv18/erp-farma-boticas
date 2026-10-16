import type { RouteObject } from 'react-router';

export const securityRoutes = [
  {
    path: 'seguridad',
    lazy: async () => {
      const { SecurityPage } = await import('./pages/SecurityPage');
      return { Component: SecurityPage };
    }
  },
  {
    path: 'seguridad/permisos',
    lazy: async () => {
      const { PermissionsPage } = await import('./pages/PermissionsPage');
      return { Component: PermissionsPage };
    }
  },
  {
    path: 'seguridad/usuarios',
    lazy: async () => {
      const { UsersPage } = await import('./pages/UsersPage');
      return { Component: UsersPage };
    }
  },
  {
    path: 'seguridad/usuarios/:userId',
    lazy: async () => {
      const { UserDetailPage } = await import('./pages/UserDetailPage');
      return { Component: UserDetailPage };
    }
  },
  {
    path: 'seguridad/roles',
    lazy: async () => {
      const { RolesPage } = await import('./pages/RolesPage');
      return { Component: RolesPage };
    }
  },
  {
    path: 'seguridad/roles/:roleId',
    lazy: async () => {
      const { RoleDetailPage } = await import('./pages/RoleDetailPage');
      return { Component: RoleDetailPage };
    }
  },
  {
    path: 'seguridad/sesiones',
    lazy: async () => {
      const { SessionsPage } = await import('./pages/SessionsPage');
      return { Component: SessionsPage };
    }
  },
  {
    path: 'seguridad/dispositivos',
    lazy: async () => {
      const { DevicesPage } = await import('./pages/DevicesPage');
      return { Component: DevicesPage };
    }
  },
  {
    path: 'seguridad/modulos',
    lazy: async () => {
      const { ModulesPage } = await import('./pages/ModulesPage');
      return { Component: ModulesPage };
    }
  }
] satisfies RouteObject[];
