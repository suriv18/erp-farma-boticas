import { enteroEnRango, useParametrosUrl } from '../../../shared/lib/use-filtros-url';

export type FiltrosProveedores = {
  estado: string;
  texto: string;
  page: number;
  size: number;
};

const PAGE_POR_DEFECTO = 0;
const SIZE_POR_DEFECTO = 20;
const SIZE_MAXIMO = 100;

export function useProveedoresFiltros() {
  const { params, actualizar } = useParametrosUrl();

  const filtros: FiltrosProveedores = {
    estado: params.get('estado') ?? '',
    texto: params.get('texto') ?? '',
    page: enteroEnRango(params.get('page'), 0, Number.MAX_SAFE_INTEGER, PAGE_POR_DEFECTO),
    size: enteroEnRango(params.get('size'), 1, SIZE_MAXIMO, SIZE_POR_DEFECTO)
  };

  return {
    filtros,
    setEstado: (estado: string) => actualizar({ estado, page: '' }),
    setTexto: (texto: string) => actualizar({ texto, page: '' }),
    setPage: (page: number) => actualizar({ page: String(page) }),
    setSize: (size: number) => actualizar({ size: String(size), page: '' })
  };
}
