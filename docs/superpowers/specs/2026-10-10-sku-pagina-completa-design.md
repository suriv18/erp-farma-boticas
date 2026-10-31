# Diseño: páginas completas para crear y editar SKU

Fecha: 2026-10-10
Estado: propuesto

## Contexto

Hoy crear y editar un SKU comercial ocurre en un modal (`size="lg"`):

- `SkusPage.tsx` abre `<Modal title="Nuevo SKU">` con `SkuForm`.
- `SkuDetailPage.tsx` abre `<Modal title="Editar SKU">` con el mismo `SkuForm`.

`SkuForm` tiene 4 grupos y 25 campos: Identificación (8), Unidades y medidas (7), Venta por fracción (3) y Control y stock (7). Es demasiado largo para un modal: el contenido se desplaza dentro de un contenedor pequeño y no hay jerarquía visual entre secciones.

Hechos verificados en el código (`frontend/apps/erp-web/src/features/catalogo/`):

1. `SkuForm` ya está separado de las páginas. Recibe `defaultValues`, `onSubmit`, `submitLabel`, `isSubmitting` y `error`, y valida con `skuSchema` (zod) mediante `react-hook-form`.
2. Los campos se declaran como `CampoFormulario[]` y se pintan con `CamposFormulario` (texto, decimal, selección, checkbox). No hay lógica condicional: todos los campos se muestran siempre.
3. Las dependencias entre campos solo se validan al enviar, en `skuSchema.superRefine`:
   - un SKU `REGULADO` exige `productoReguladoId`;
   - `permiteVentaFraccion` exige `factorFraccion` y `unidadFraccionCodigo`;
   - sin `permiteVentaFraccion`, `factorFraccion` debe estar vacío.
4. `toSkuPayload` y `toSkuFormValues` (`lib/sku-form.ts`) convierten entre formulario y API; `SKU_VACIO` es el valor inicial.
5. Las rutas `catalogo/skus` y `catalogo/skus/:skuId` están en `routes.tsx`. El patrón de página completa para "crear X" ya existe en seguridad (`NewUserPage`, spec `2026-09-27-crear-usuario-pagina-completa-design.md`).
6. No existe un e2e de Playwright para catálogo.

## Alcance

1. Crear con una página completa en `/catalogo/skus/nuevo` y editar con otra en `/catalogo/skus/:skuId/editar`.
2. Rediseñar `SkuForm` en tarjetas por sección, con barra de acciones fija.
3. Mostrar solo los campos que aplican (producto regulado y fracción) y vaciar los que se ocultan.
4. Eliminar los dos modales (creación y edición) y sustituirlos por enlaces.

**Fuera de alcance:**

- Cambio de estado del SKU: sigue en `EstadoDialog` desde el detalle.
- Códigos de barra (`CodigosBarraSection`) y su formulario.
- Backend, `skuSchema` y `toSkuPayload` (solo se reutilizan).
- Aviso de "cambios sin guardar" al salir de la página.
- Tests e2e de Playwright.

## Diseño

### Rutas y navegación

En `routes.tsx` se añaden, antes de `catalogo/skus/:skuId` para que `nuevo` no se interprete como un id:

| Ruta | Página |
| --- | --- |
| `catalogo/skus/nuevo` | `NuevoSkuPage` |
| `catalogo/skus/:skuId/editar` | `EditarSkuPage` |

Ambas con `lazy`, igual que el resto de rutas de la feature.

- `SkusPage`: el botón "Nuevo SKU" pasa a ser un enlace a `/catalogo/skus/nuevo`. Se elimina el `Modal`, `createOpen`, `createMutation` y las importaciones que queden sin uso.
- `SkuDetailPage`: el botón "Editar" pasa a ser un enlace a `/catalogo/skus/:skuId/editar`. Se elimina el modal de edición y `updateMutation`. El diálogo de estado no cambia.
- Tras guardar con éxito (crear o editar) se navega a `/catalogo/skus` y se invalida `['catalogo', 'skus']`. "Cancelar" navega a `/catalogo/skus`.

### Formulario (`SkuForm`)

Mantiene las props actuales y añade `onCancel: () => void`. Se vuelve un formulario de página:

- Las 4 secciones se pintan como tarjetas (`Card` de `@boticas/ui-web`) apiladas, cada una con su título y una rejilla de 1 columna en móvil y 2–3 en escritorio.
- El error del servidor (`FormError`) se muestra arriba, antes de la primera tarjeta.
- Barra de acciones fija en el borde inferior con "Cancelar" (secundario) y el botón de envío (`submitLabel`, deshabilitado mientras `isSubmitting`). La barra no tapa el último campo: el contenedor reserva espacio inferior igual a su altura.
- Etiquetas, tipos de campo, valores iniciales y mensajes de validación no cambian.

### Campos condicionales

La visibilidad se calcula con `useWatch` sobre `tipoSku` y `permiteVentaFraccion`:

| Campo(s) | Se muestra cuando |
| --- | --- |
| `productoReguladoId` | `tipoSku === 'REGULADO'` |
| `factorFraccion`, `unidadFraccionCodigo` | `permiteVentaFraccion === true` |

Los valores de los campos ocultos se descartan antes de validar y de enviar: una función pura `aplicarVisibilidad(values)` los pone en `''` (`productoReguladoId` si el tipo no es REGULADO; `factorFraccion` y `unidadFraccionCodigo` si no se permite fracción). El resolver del formulario la aplica a los valores antes de pasarlos a `skuSchema`, y `handleSubmit` entrega al `onSubmit` el resultado ya normalizado.

Con esto el valor enviado es siempre coherente con lo que ve el usuario y `skuSchema` no necesita cambios. No se usan efectos ni `setValue`, y `CamposFormulario` no cambia. Si el usuario vuelve a activar la opción antes de guardar, el valor que había escrito reaparece.

Por esa condicionalidad, la definición de grupos pasa a una función pura que recibe los valores observados y devuelve los campos visibles, en `lib/sku-secciones.ts`, junto con `aplicarVisibilidad`. Así se prueban sin renderizar.

### Páginas

- `NuevoSkuPage` (`pages/NuevoSkuPage.tsx`): `PageHeader` con "Catálogo / SKU / Nuevo SKU"; `SkuForm` con `SKU_VACIO`, `submitLabel="Crear SKU"`; mutación `crearSku(apiClient, toSkuPayload(values))`; `describeApiError` para el error.
- `EditarSkuPage` (`pages/EditarSkuPage.tsx`): obtiene `skuId` con `useRouteParam('skuId')` y el SKU con `skuQuery(skuId)`.
  - Mientras carga muestra "Cargando…"; si falla, `FormError` con `describeApiError`.
  - Con datos renderiza `SkuForm` con `defaultValues={toSkuFormValues(sku)}`, `submitLabel="Guardar cambios"` y la mutación `actualizarSku(apiClient, skuId, toSkuPayload(values))`.
  - El formulario se monta una vez que el SKU está disponible, para que `defaultValues` sea el del servidor.

### Flujo de datos

```
SkusPage ──Link──▶ NuevoSkuPage ──crearSku──▶ invalidar ['catalogo','skus'] ──▶ /catalogo/skus
SkuDetailPage ──Link──▶ EditarSkuPage ──skuQuery──▶ SkuForm ──actualizarSku──▶ invalidar ──▶ /catalogo/skus
```

### Manejo de errores

- Errores del backend al guardar: se muestran en el `FormError` superior con `describeApiError`; el formulario conserva lo escrito.
- Error de carga en edición: `FormError` en lugar del formulario.
- Un `skuId` inexistente produce el error de carga del backend y se muestra igual.

## Pruebas

Cada archivo nuevo debe llegar al 100% de líneas y ramas (gate de Vitest). Pruebas con el estilo del repo (`testing-library`, `msw`, `userEvent`):

- `lib/sku-secciones.test.ts`: campos visibles según `tipoSku` y `permiteVentaFraccion`, las cuatro combinaciones.
- `SkuForm.test.tsx` (actualizar): las secciones se muestran como tarjetas; producto regulado aparece solo con tipo REGULADO; factor y unidad de fracción solo con fracción; al ocultarlos se vacían y no se envían; botón Cancelar llama a `onCancel`; el envío usa `submitLabel` y se deshabilita en `isSubmitting`; el error se muestra arriba.
- `NuevoSkuPage.test.tsx`: crea sin enviar tenant, navega a `/catalogo/skus` e invalida la caché; muestra el error del servidor; Cancelar vuelve al listado.
- `EditarSkuPage.test.tsx`: estado de carga; error de carga; rellena con los datos del SKU; guarda con `actualizarSku`, navega y muestra el error del servidor.
- `SkusPage.test.tsx` y `SkuDetailPage.test.tsx` (actualizar): "Nuevo SKU" y "Editar" son enlaces a las rutas nuevas; se retiran las pruebas del modal.
- `routes`: las dos rutas nuevas están registradas y `nuevo` queda antes de `:skuId`.

Verificación final: `pnpm check` completo en el frontend, más una comprobación en navegador contra el backend real de crear y editar un SKU, con y sin producto regulado y con y sin fracción.

## Decisiones tomadas

- **Crear y editar comparten diseño.** Evita mantener dos formularios distintos.
- **Una sola página, tarjetas apiladas.** Sin índice lateral ni asistente por pasos: son 4 secciones y todo cabe en una lectura.
- **Mostrar solo lo que aplica.** Reduce el ruido y evita enviar valores que el esquema rechaza.
- **Volver al listado tras guardar**, tanto al crear como al editar.
