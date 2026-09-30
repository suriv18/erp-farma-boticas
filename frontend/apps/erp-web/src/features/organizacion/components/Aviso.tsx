import type { PropsWithChildren } from 'react';
import { ESTILO_AVISO } from './estilo-aviso';

export function Aviso({ children }: PropsWithChildren) {
  return (
    <p role="status" className={`${ESTILO_AVISO} mt-6 px-4`}>
      {children}
    </p>
  );
}
