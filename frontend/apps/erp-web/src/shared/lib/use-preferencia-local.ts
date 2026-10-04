import { useState } from 'react';

function leer<T>(clave: string, inicial: T): T {
  try {
    const guardado = localStorage.getItem(clave);
    return guardado === null ? inicial : (JSON.parse(guardado) as T);
  } catch {
    return inicial;
  }
}

export function usePreferenciaLocal<T>(
  clave: string,
  inicial: T
): readonly [T, (valor: T) => void] {
  const [valor, setValor] = useState<T>(() => leer(clave, inicial));

  return [
    valor,
    (nuevo) => {
      setValor(nuevo);
      try {
        localStorage.setItem(clave, JSON.stringify(nuevo));
      } catch {
        return;
      }
    }
  ] as const;
}
