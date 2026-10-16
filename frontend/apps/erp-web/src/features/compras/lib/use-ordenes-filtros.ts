import { enteroEnRango, useParametrosUrl } from '../../../shared/lib/use-filtros-url';

export type FiltrosOrdenesUrl = {
  proveedorId: string;
  estado: string;
  page: number;
  size: number;
};

const PAGE_POR_DEFECTO = 0;
const SIZE_POR_DEFECTO = 20;
const SIZE_MAXIMO = 100;

export function useOrdenesFiltros() {
  const { params, actualizar } = useParametrosUrl();

  const filtros: FiltrosOrdenesUrl = {
    proveedorId: params.get('proveedorId') ?? '',
    estado: params.get('estado') ?? '',
    page: enteroEnRango(params.get('page'), 0, Number.MAX_SAFE_INTEGER, PAGE_POR_DEFECTO),
    size: enteroEnRango(params.get('size'), 1, SIZE_MAXIMO, SIZE_POR_DEFECTO)
  };

  return {
    filtros,
    setProveedor: (proveedorId: string) => actualizar({ proveedorId, page: '' }),
    setEstado: (estado: string) => actualizar({ estado, page: '' }),
    setPage: (page: number) => actualizar({ page: String(page) }),
    setSize: (size: number) => actualizar({ size: String(size), page: '' })
  };
}
