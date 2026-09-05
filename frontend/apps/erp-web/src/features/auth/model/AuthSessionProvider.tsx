import { useMemo, useState, type PropsWithChildren } from 'react';
import { AuthSessionContext, type AuthSession } from './auth-session.context';

export function AuthSessionProvider({ children }: PropsWithChildren) {
  const [authenticated, setAuthenticated] = useState(false);

  const session = useMemo<AuthSession>(
    () => ({
      authenticated,
      authenticate: () => {
        setAuthenticated(true);
        return Promise.resolve();
      },
      signOut: () => setAuthenticated(false)
    }),
    [authenticated]
  );

  return <AuthSessionContext value={session}>{children}</AuthSessionContext>;
}
