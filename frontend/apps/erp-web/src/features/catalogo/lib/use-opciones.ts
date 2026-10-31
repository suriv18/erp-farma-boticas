import { useQuery } from '@tanstack/react-query';
import { fetchCategorias } from '../api/categorias.api';
import { fetchMarcas } from '../api/marcas.api';
import { principiosActivosQuery } from '../api/principios-activos.api';
import { fetchProductosRegulados } from '../api/productos-regulados.api';
import type { Opcion } from '../components/CamposFormulario';
import type { SupportCatalogApi, SupportCatalogItem } from '../support-catalog';

import { apiClient } from '../../../app/api';

export async function cargarOpciones<T>(
  fetchPage: (page: number) => Promise<{ items: T[]; totalElements: number }>
): Promise<T[]> {
  const items: T[] = [];
  for (let page = 0; ; page++) {
    const result = await fetchPage(page);
    if (!result.items.length && items.length < result.totalElements)
      throw new Error('No se pudieron completar las opciones. Reintenta la consulta.');
    items.push(...result.items);
    if (items.length >= result.totalElements) return items;
  }
}

export function opcionesDe<T>(
  items: readonly T[] | undefined,
  valor: (item: T) => string,
  etiqueta: (item: T) => string
): Opcion[] {
  return (items ?? []).map((item) => ({ value: valor(item), label: etiqueta(item) }));
}

export function opcionesDeValores(valores: readonly string[]): Opcion[] {
  return valores.map((valor) => ({ value: valor, label: valor }));
}

export function etiquetaDe(opciones: readonly Opcion[], valor: string): string {
  return opciones.find((opcion) => opcion.value === valor)?.label ?? valor;
}

export function useSoporteOpcionesQuery<T extends SupportCatalogItem & { denominacion: string }>(
  api: Pick<SupportCatalogApi<T, unknown>, 'listQuery'>
) {
  return useQuery({
    ...api.listQuery('ACTIVO'),
    select: (data) =>
      opcionesDe(
        data,
        (item) => item.codigo,
        (item) => `${item.codigo} — ${item.denominacion}`
      )
  });
}

export function useMarcasOpcionesQuery() {
  return useQuery({
    queryKey: ['catalogo', 'marcas', 'opciones'],
    queryFn: async () =>
      opcionesDe(
        await cargarOpciones((page) =>
          fetchMarcas(apiClient, { estado: 'ACTIVO', page, size: 100 })
        ),
        (item) => item.id,
        (item) => item.nombre
      )
  });
}
export function useCategoriasOpcionesQuery() {
  return useQuery({
    queryKey: ['catalogo', 'categorias', 'opciones'],
    queryFn: async () =>
      opcionesDe(
        await cargarOpciones((page) =>
          fetchCategorias(apiClient, { estado: 'ACTIVO', page, size: 100 })
        ),
        (item) => item.id,
        (item) => item.nombre
      )
  });
}
export function useProductosOpcionesQuery() {
  return useQuery({
    queryKey: ['catalogo', 'productos-regulados', 'opciones'],
    queryFn: async () =>
      opcionesDe(
        await cargarOpciones((page) => fetchProductosRegulados(apiClient, { page, size: 100 })),
        (item) => item.id,
        (item) => item.denominacion
      )
  });
}
export function useOpcionesMarcas(): Opcion[] {
  return useMarcasOpcionesQuery().data ?? [];
}
export function useOpcionesCategorias(): Opcion[] {
  return useCategoriasOpcionesQuery().data ?? [];
}
export function useOpcionesProductosRegulados(): Opcion[] {
  return useProductosOpcionesQuery().data ?? [];
}

export function useOpcionesPrincipiosActivos(): Opcion[] {
  const { data } = useQuery(principiosActivosQuery({ estado: 'ACTIVO' }));
  return opcionesDe(
    data,
    (principio) => principio.id,
    (principio) => principio.denominacion
  );
}

export function useOpcionesSoporte<T extends SupportCatalogItem & { denominacion: string }>(
  api: Pick<SupportCatalogApi<T, unknown>, 'listQuery'>
): Opcion[] {
  return useSoporteOpcionesQuery(api).data ?? [];
}
