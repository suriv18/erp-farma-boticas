import { Badge } from '@boticas/ui-web';

const SUCCESS_STATUSES = new Set(['ACTIVO', 'ACTIVE', 'CONFIABLE', 'TRUSTED']);
const DANGER_STATUSES = new Set(['INACTIVO', 'INACTIVE', 'REVOCADO', 'REVOKED', 'BLOQUEADO', 'BLOCKED']);
const WARNING_STATUSES = new Set(['PENDIENTE', 'PENDING', 'SUSPENDIDO', 'SUSPENDED']);

function toneFor(status: string): 'success' | 'danger' | 'warning' | 'neutral' {
  const normalized = status.toUpperCase();
  if (SUCCESS_STATUSES.has(normalized)) return 'success';
  if (DANGER_STATUSES.has(normalized)) return 'danger';
  if (WARNING_STATUSES.has(normalized)) return 'warning';
  return 'neutral';
}

export function EstadoBadge({ status }: { status: string }) {
  return <Badge tone={toneFor(status)}>{status}</Badge>;
}
