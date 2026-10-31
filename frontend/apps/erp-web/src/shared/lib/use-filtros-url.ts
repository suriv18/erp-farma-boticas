import { useEffect, useRef } from 'react';
import { useSearchParams } from 'react-router';

export const enteroEnRango = (
  valor: string | null,
  minimo: number,
  maximo: number,
  defecto: number
) => {
  const numero = valor === null || valor === '' ? Number.NaN : Number(valor);
  return Number.isInteger(numero) && numero >= minimo && numero <= maximo ? numero : defecto;
};

export function useParametrosUrl() {
  const [params, setParams] = useSearchParams();

  const vigentes = useRef(params);
  useEffect(() => {
    vigentes.current = params;
  }, [params]);

  const actualizar = (cambios: Record<string, string>) => {
    const siguientes = new URLSearchParams(vigentes.current);
    Object.entries(cambios).forEach(([clave, valor]) =>
      valor === '' ? siguientes.delete(clave) : siguientes.set(clave, valor)
    );
    vigentes.current = siguientes;
    setParams(siguientes);
  };

  return { params, actualizar };
}
