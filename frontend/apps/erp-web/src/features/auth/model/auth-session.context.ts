import { createContext } from 'react';
import type { LoginCredentials } from '../schemas/login.schema';

export type AuthSession = {
  authenticated: boolean;
  authenticate: (credentials: LoginCredentials) => Promise<void>;
  signOut: () => void;
};

export const AuthSessionContext = createContext<AuthSession | null>(null);
