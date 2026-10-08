export const PAGINA_POR_DEFECTO = 0;
export const TAMANO_POR_DEFECTO = 20;

export type ParametrosLista = {
  search?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function resolverPaginacion(params: Pick<ParametrosLista, 'page' | 'size'>): {
  page: number;
  size: number;
} {
  return {
    page: params.page ?? PAGINA_POR_DEFECTO,
    size: params.size ?? TAMANO_POR_DEFECTO
  };
}
