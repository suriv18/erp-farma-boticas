import { useContext } from 'react';
import { AuthSessionContext } from './auth-session.context';

export function useAuthSession() {
  const session = useContext(AuthSessionContext);

  if (!session) {
    throw new Error('useAuthSession debe utilizarse dentro de AuthSessionProvider.');
  }

  return session;
}
