import { useRef } from 'react';
import { nuevaClaveIdempotencia } from './idempotencia';

type ClaveGuardada = { firma: string; clave: string };

export function useClaveIdempotencia() {
  const guardada = useRef<ClaveGuardada | null>(null);

  return {
    claveDe: (payload: unknown): string => {
      const firma = JSON.stringify(payload);
      if (guardada.current?.firma === firma) return guardada.current.clave;
      const clave = nuevaClaveIdempotencia();
      guardada.current = { firma, clave };
      return clave;
    },
    reiniciar: () => {
      guardada.current = null;
    }
  };
}
