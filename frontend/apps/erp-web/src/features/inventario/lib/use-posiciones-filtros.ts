import { useEffect, useRef } from 'react';
import { useSearchParams } from 'react-router';

export type FiltrosPosiciones = {
  establecimientoId: string;
  almacenId: string;
  skuId: string;
  page: number;
  size: number;
};

export function usePosicionesFiltros() {
  const [params, setParams] = useSearchParams();

  const filtros: FiltrosPosiciones = {
    establecimientoId: params.get('establecimientoId') ?? '',
    almacenId: params.get('almacenId') ?? '',
    skuId: params.get('skuId') ?? '',
    page: Number(params.get('page') ?? 0),
    size: Number(params.get('size') ?? 20)
  };

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

  return {
    filtros,
    setEstablecimiento: (id: string) =>
      actualizar({ establecimientoId: id, almacenId: '', page: '' }),
    setAlmacen: (id: string) => actualizar({ almacenId: id, page: '' }),
    setSku: (id: string) => actualizar({ skuId: id, page: '' }),
    setPage: (page: number) => actualizar({ page: String(page) }),
    setSize: (size: number) => actualizar({ size: String(size), page: '' })
  };
}
