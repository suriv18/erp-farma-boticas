import { createContext } from 'react';
import type { ChangePasswordRequest } from '../api/auth-tokens.types';
import type { LoginCredentials } from '../schemas/login.schema';

export type AuthSessionStatus = 'loading' | 'authenticated' | 'unauthenticated';

export type AuthSession = {
  status: AuthSessionStatus;
  authenticated: boolean;
  accessToken: string | null;
  tenantId: string | null;
  userId: string | null;
  passwordChangeRequired: boolean;
  authenticate: (credentials: LoginCredentials) => Promise<void>;
  changePassword: (request: ChangePasswordRequest) => Promise<void>;
  signOut: () => Promise<void>;
};

export const AuthSessionContext = createContext<AuthSession | null>(null);
