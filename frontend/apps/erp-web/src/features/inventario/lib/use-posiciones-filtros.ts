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

  const actualizar = (cambios: Record<string, string>) =>
    setParams((actuales) => {
      const siguientes = new URLSearchParams(actuales);
      Object.entries(cambios).forEach(([clave, valor]) =>
        valor === '' ? siguientes.delete(clave) : siguientes.set(clave, valor)
      );
      return siguientes;
    });

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
