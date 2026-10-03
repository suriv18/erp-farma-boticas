import { useQuery } from '@tanstack/react-query';
import { categoriasQuery } from '../api/categorias.api';
import { marcasQuery } from '../api/marcas.api';
import { principiosActivosQuery } from '../api/principios-activos.api';
import { productosReguladosQuery } from '../api/productos-regulados.api';
import type { Opcion } from '../components/CamposFormulario';
import type { SupportCatalogApi, SupportCatalogItem } from '../support-catalog';

const MAX_OPCIONES = 100;

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

export function useOpcionesSoporte<T extends SupportCatalogItem & { denominacion: string }>(
  api: Pick<SupportCatalogApi<T, unknown>, 'listQuery'>
): Opcion[] {
  const { data } = useQuery(api.listQuery('ACTIVO'));
  return opcionesDe(
    data,
    (item) => item.codigo,
    (item) => `${item.codigo} — ${item.denominacion}`
  );
}

export function useOpcionesMarcas(): Opcion[] {
  const { data } = useQuery(marcasQuery({ estado: 'ACTIVO', size: MAX_OPCIONES }));
  return opcionesDe(
    data?.items,
    (marca) => marca.id,
    (marca) => marca.nombre
  );
}

export function useOpcionesCategorias(): Opcion[] {
  const { data } = useQuery(categoriasQuery({ estado: 'ACTIVO', size: MAX_OPCIONES }));
  return opcionesDe(
    data?.items,
    (categoria) => categoria.id,
    (categoria) => categoria.nombre
  );
}

export function useOpcionesProductosRegulados(): Opcion[] {
  const { data } = useQuery(productosReguladosQuery({ size: MAX_OPCIONES }));
  return opcionesDe(
    data?.items,
    (producto) => producto.id,
    (producto) => producto.denominacion
  );
}

export function useOpcionesPrincipiosActivos(): Opcion[] {
  const { data } = useQuery(principiosActivosQuery({ estado: 'ACTIVO' }));
  return opcionesDe(
    data,
    (principio) => principio.id,
    (principio) => principio.denominacion
  );
}
