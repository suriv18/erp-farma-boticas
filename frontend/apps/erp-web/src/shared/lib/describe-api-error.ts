import { ApiError } from '@boticas/api-client';

export function describeApiError(error: unknown): string {
  if (!(error instanceof ApiError)) return 'No se pudo completar la operación. Inténtalo de nuevo.';
  if (error.status === 403) return 'No tienes permiso para esta acción.';
  if (error.status === 404) return 'El recurso no existe o no pertenece a tu organización.';
  return error.problem?.detail ?? error.problem?.title ?? error.message;
}
