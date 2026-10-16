import { enteroEnRango, useParametrosUrl } from '../../../shared/lib/use-filtros-url';

export type FiltrosPosiciones = {
  establecimientoId: string;
  almacenId: string;
  skuId: string;
  page: number;
  size: number;
};

const PAGE_POR_DEFECTO = 0;
const SIZE_POR_DEFECTO = 20;
const SIZE_MAXIMO = 100;

export function usePosicionesFiltros() {
  const { params, actualizar } = useParametrosUrl();

  const filtros: FiltrosPosiciones = {
    establecimientoId: params.get('establecimientoId') ?? '',
    almacenId: params.get('almacenId') ?? '',
    skuId: params.get('skuId') ?? '',
    page: enteroEnRango(params.get('page'), 0, Number.MAX_SAFE_INTEGER, PAGE_POR_DEFECTO),
    size: enteroEnRango(params.get('size'), 1, SIZE_MAXIMO, SIZE_POR_DEFECTO)
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
