import { enteroEnRango, useParametrosUrl } from '../../../shared/lib/use-filtros-url';

export type FiltrosVentas = {
  establecimientoId: string;
  desde: string;
  hasta: string;
  page: number;
  size: number;
};

const PAGE_POR_DEFECTO = 0;
const SIZE_POR_DEFECTO = 20;
const SIZE_MAXIMO = 100;

export function useVentasFiltros() {
  const { params, actualizar } = useParametrosUrl();

  const filtros: FiltrosVentas = {
    establecimientoId: params.get('establecimientoId') ?? '',
    desde: params.get('desde') ?? '',
    hasta: params.get('hasta') ?? '',
    page: enteroEnRango(params.get('page'), 0, Number.MAX_SAFE_INTEGER, PAGE_POR_DEFECTO),
    size: enteroEnRango(params.get('size'), 1, SIZE_MAXIMO, SIZE_POR_DEFECTO)
  };

  return {
    filtros,
    setEstablecimiento: (id: string) => actualizar({ establecimientoId: id, page: '' }),
    setDesde: (fecha: string) => actualizar({ desde: fecha, page: '' }),
    setHasta: (fecha: string) => actualizar({ hasta: fecha, page: '' }),
    setPage: (page: number) => actualizar({ page: String(page) }),
    setSize: (size: number) => actualizar({ size: String(size), page: '' })
  };
}
