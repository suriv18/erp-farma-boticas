import { useAuthSession } from '../../auth';

export function useTenantId(): string {
  return useAuthSession().tenantId ?? '';
}
