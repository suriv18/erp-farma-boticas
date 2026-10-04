import { useRef } from 'react';
import { nuevaClaveIdempotencia } from './idempotencia';

type ClaveGuardada = { firma: string; clave: string };

export function useClaveIdempotencia(): (payload: unknown) => string {
  const guardada = useRef<ClaveGuardada | null>(null);

  return (payload) => {
    const firma = JSON.stringify(payload);
    if (guardada.current?.firma === firma) return guardada.current.clave;
    const clave = nuevaClaveIdempotencia();
    guardada.current = { firma, clave };
    return clave;
  };
}
