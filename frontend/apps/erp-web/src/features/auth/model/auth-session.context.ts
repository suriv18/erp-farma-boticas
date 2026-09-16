import { createContext } from 'react';
import type { LoginCredentials } from '../schemas/login.schema';

export type AuthSession = {
  authenticated: boolean;
  accessToken: string | null;
  tenantId: string | null;
  userId: string | null;
  authenticate: (credentials: LoginCredentials) => Promise<void>;
  signOut: () => Promise<void>;
};

export const AuthSessionContext = createContext<AuthSession | null>(null);
