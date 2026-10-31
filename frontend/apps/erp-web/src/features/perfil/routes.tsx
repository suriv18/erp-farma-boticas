import type { RouteObject } from 'react-router';

export const profileRoutes = [
  {
    path: 'perfil',
    lazy: async () => {
      const { MyProfilePage } = await import('./pages/MyProfilePage');
      return { Component: MyProfilePage };
    }
  },
  {
    path: 'perfil/seguridad',
    lazy: async () => {
      const { AccountSecurityPage } = await import('./pages/AccountSecurityPage');
      return { Component: AccountSecurityPage };
    }
  },
  {
    path: 'perfil/configuraciones',
    lazy: async () => {
      const { SettingsPage } = await import('./pages/SettingsPage');
      return { Component: SettingsPage };
    }
  },
  {
    path: 'perfil/sucursal',
    lazy: async () => {
      const { SwitchBranchPage } = await import('./pages/SwitchBranchPage');
      return { Component: SwitchBranchPage };
    }
  },
  {
    path: 'perfil/ayuda',
    lazy: async () => {
      const { HelpPage } = await import('./pages/HelpPage');
      return { Component: HelpPage };
    }
  }
] satisfies RouteObject[];
