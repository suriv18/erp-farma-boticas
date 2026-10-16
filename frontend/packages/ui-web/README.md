# Patrón compartido de listados

Los módulos importan los componentes desde `@boticas/ui-web`. No deben copiar el
marcado del encabezado, buscador, tabla ni pie de paginación.

- `PageHeader`: contexto, título, descripción y acciones del módulo; se apila en móvil.
- `ListFilters`: buscador etiquetado y espacio para filtros específicos como estado.
- `DataTable<T>`: columnas, filas, carga, error y vacío; desplazamiento horizontal
  dentro de la tabla y paginación integrada debajo, fuera del área desplazable.
- `Pagination`: componente controlado compartido. Se puede utilizar de forma
  independiente cuando el contenido no es una tabla.
- `FormField`, `Modal` y `Button`: componentes existentes para formularios y acciones.

## Listado con API paginada

```tsx
<PageHeader
  context="Catálogo / Marcas"
  title="Marcas"
  description="Administra las marcas comerciales."
  actions={<Button onClick={openCreate}>Nueva marca</Button>}
/>
<ListFilters
  label="Buscar marca"
  placeholder="Código o nombre"
  value={search}
  onValueChange={(value) => {
    setSearch(value);
    setPage(0);
  }}
/>
<DataTable
  columns={columns}
  rows={data?.items ?? []}
  rowKey={(row) => row.id}
  emptyMessage="No se encontraron marcas."
  isLoading={isPending}
  isError={isError}
  startIndex={page * size}
  pagination={{
    page,
    size,
    totalElements: data?.totalElements ?? 0,
    onPageChange: setPage,
    onSizeChange: setSize,
  }}
/>
```

## Contrato

- `page` es base cero para ser compatible con la API. El usuario ve base uno.
- `size` es un entero positivo. El valor inicial actual es 20; se ofrecen 20, 50
  y 100, conservando también un tamaño personalizado del consumidor.
- `totalElements` es el total filtrado, no la cantidad de filas de la página.
- El cambio de tamaño notifica el nuevo tamaño y reinicia la página a cero.
- Si el total disminuye, el paginador notifica la última página válida. Durante
  carga o error los controles y esta corrección permanecen deshabilitados.
- Cada módulo sigue siendo dueño de su consulta, claves de caché, filtros,
  permisos y mutaciones. La tabla no crea peticiones ni pagina nuevamente una
  respuesta que ya viene paginada del servidor.
- Para catálogos pequeños que devuelven un array completo, el consumidor filtra
  primero, calcula el total y entrega a la tabla solamente el segmento visible.
- No añadir un segundo `Pagination` junto a una `DataTable` con `pagination`.
- El export antiguo de Catálogo es un puente de compatibilidad, no otra implementación.

Los listados de marcas, categorías, rubros comerciales, usuarios y roles usan la
paginación del servidor. Los catálogos auxiliares y permisos usan paginación local.

La persistencia de filtros en URL y el ordenamiento remoto son extensiones
posteriores; esta refactorización no cambia los contratos de búsqueda de la API.
