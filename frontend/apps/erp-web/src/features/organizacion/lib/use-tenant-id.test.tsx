import { renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';
import { AuthSessionContext, type AuthSession } from '../../auth/model/auth-session.context';
import { useTenantId } from './use-tenant-id';

function sessionWith(tenantId: string | null): AuthSession {
  return {
    status: 'authenticated',
    authenticated: true,
    accessToken: 'token',
    tenantId,
    userId: 'user-1',
    authenticate: vi.fn(),
    signOut: vi.fn()
  };
}

function wrapperFor(session: AuthSession) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return <AuthSessionContext value={session}>{children}</AuthSessionContext>;
  };
}

describe('useTenantId', () => {
  it('devuelve el tenant de la sesión', () => {
    const { result } = renderHook(() => useTenantId(), {
      wrapper: wrapperFor(sessionWith('tenant-1'))
    });
    expect(result.current).toBe('tenant-1');
  });

  it('devuelve cadena vacía cuando la sesión no tiene tenant', () => {
    const { result } = renderHook(() => useTenantId(), { wrapper: wrapperFor(sessionWith(null)) });
    expect(result.current).toBe('');
  });
});
