# UI de Organización (Empresa, Establecimiento, Almacén, Terminal POS) y cambio de estado — Diseño

## Contexto

El backend del módulo `organizacion` ya expone CRUD REST real para Empresa Operadora, Establecimiento, Almacén y
Terminal POS bajo `/api/v1/organizacion/*`, más `GET /api/v1/estructura-corporativa` (ver
`2026-09-27-api-organizacion-design.md`). Está verificado contra PostgreSQL real (Testcontainers y stack Docker local).

El frontend (`frontend/apps/erp-web/src/features/organizacion/`) solo tiene `OrganizationPage`, una página de solo
lectura con el resumen y el árbol de `GET /estructura-corporativa`. No existe UI para crear, editar ni cambiar el
estado de ninguna de las cuatro entidades; hoy solo se pueden gestionar por API.

Además, la API no permite cambiar el estado de una Empresa ni de un Establecimiento: sus `PUT` no reciben el campo
`estado`, aunque los agregados de dominio ya tienen `cambiarEstado` y `cambiarEstadoOperativo`. Almacén (`activo`) y
Terminal (`estado`) sí lo cambian dentro de su `PUT`.

Este spec cubre dos entregas dependientes en una sola serie: (1) endpoints de cambio de estado en el backend y
(2) la UI completa de gestión en el frontend.

## Decisiones de alcance (confirmadas)

- **Navegación jerárquica (drill-down):** Empresas → detalle de empresa (sus establecimientos) → detalle de
  establecimiento (sus almacenes y terminales). Se descartaron las pestañas con listas planas y las cuatro páginas
  CRUD independientes.
- **Cambio de estado de Empresa y Establecimiento:** se amplía el backend con `PATCH .../estado`; no se deja como
  solo lectura.
- **Formularios completos:** cada formulario expone todos los campos que acepta la API. El de Establecimiento se
  agrupa en secciones (Identificación, Regulatorio, Ubicación, Operación).
- **Enfoque de frontend:** páginas y formularios dedicados por entidad, siguiendo `seguridad` y `catalogo`. Se
  descartó un CRUD genérico dirigido por configuración (`support-catalog`) porque las entidades difieren demasiado
  en campos, jerarquía y estados.
- **Sin control de UI por permisos:** el frontend no tiene noción de permisos (no existe `hasPermission`). Los
  botones se muestran siempre y el backend responde 403 cuando falta permiso.

## Fase 1 — Backend: cambio de estado

### API

| Método y ruta | Cuerpo | Respuesta |
|---|---|---|
| `PATCH /api/v1/organizacion/empresas/{empresaId}/estado?tenantId=` | `{ "estado": "SUSPENDIDO" }` | `200` con `EmpresaOperadoraResponse` |
| `PATCH /api/v1/organizacion/establecimientos/{establecimientoId}/estado?tenantId=` | `{ "estado": "CLAUSURADO" }` | `200` con `EstablecimientoResponse` |

- Valores válidos, tomados de los enums de dominio: Empresa `ACTIVO | SUSPENDIDO | BLOQUEADO`; Establecimiento
  `ACTIVO | SUSPENDIDO | CLAUSURADO | REMODELACION`.
- Estado desconocido o vacío → `400`. Recurso inexistente o de otro tenant → `404`.
- Permisos: se reutilizan `organizacion.empresas.gestionar` y `organizacion.establecimientos.gestionar`. No hay
  migración de permisos.

### Aplicación

- `CambiarEstadoEmpresaUseCase` y `CambiarEstadoEstablecimientoUseCase`, con handlers
  `(OrganizacionReadPort, OrganizacionWritePort, ClockPort)`, la misma firma que los `Actualizar*Handler`.
- Cada handler lee el agregado, aplica `cambiarEstado` / `cambiarEstadoOperativo` y persiste con el `save` existente.
  No hay cambios de persistencia ni de esquema.
- Comandos nuevos: `CambiarEstadoEmpresaCommand(empresaId, tenantId, estado)` y
  `CambiarEstadoEstablecimientoCommand(establecimientoId, tenantId, estado)`.
- Los beans se registran en `OrganizacionModuleConfiguration`.

### Fuera de alcance de la fase

- No se validan transiciones entre estados (por ejemplo `CLAUSURADO` → `ACTIVO`): el dominio hoy acepta cualquier
  cambio.
- No se propaga el estado a las entidades hijas: suspender una empresa no suspende sus establecimientos.

### Pruebas

- Unitarias al 100% de líneas y ramas (handlers, mapper, controller), con los mismos fixtures y aserciones
  compartidas que `EmpresaOperadoraControllerTest` y `EstablecimientoControllerTest`.
- Casos nuevos en `OrganizacionApiIntegrationTest` (login real, PostgreSQL): cambio válido, estado inválido → 400,
  recurso ajeno o inexistente → 404, sin permiso → 403.
- Se actualiza `CLAUDE.md` (sección "Estado real del proyecto").

## Fase 2 — Frontend

### Rutas

Se agregan a `features/organizacion/routes.tsx` (lazy, como `seguridad`):

- `/organizacion`: se conserva el resumen y el árbol. Los nombres de empresa y establecimiento son enlaces a su
  detalle y se agrega un botón "Gestionar empresas".
- `/organizacion/empresas`: lista con búsqueda, paginación y "Nueva empresa".
- `/organizacion/empresas/:empresaId`: datos de la empresa con Editar y Cambiar estado, y tabla de sus
  establecimientos con "Nuevo establecimiento".
- `/organizacion/establecimientos/:establecimientoId`: datos con Editar y Cambiar estado, y dos tablas: Almacenes y
  Terminales, cada una con su botón "Nuevo".

`features/organizacion/index.ts` sigue siendo la única superficie pública; otras features no importan archivos
internos.

### Capa de datos

- Un archivo por entidad, como en `seguridad`: `empresas.api.ts`, `establecimientos.api.ts`, `almacenes.api.ts`,
  `terminales.api.ts`, cada uno con su `.types.ts`.
- Cada `.api.ts` trae `queryOptions`, `crear*`, `actualizar*` y, solo para Empresa y Establecimiento,
  `cambiarEstado*`.
- El `tenantId` sale de `useAuthSession()`. Viaja en el body de los `POST` (los `Crear*Request` lo exigen) y como
  query param en `GET`, `PUT` y `PATCH`.
- Toda mutación invalida la lista de su entidad y `['organization', 'corporate-structure']`, para que el resumen y
  los selects de `AsignacionRolForm` se refresquen.
- Las listas paginadas usan `PaginaResponse<T>` (`items`, `page`, `size`, `totalElements`).

### Formularios

- Uno por entidad, en `Modal`, con react-hook-form y schemas zod (`schemas/*.schema.ts`).
- Los schemas repiten los límites del DDL y de los DTO del backend: RUC de 11 dígitos, series de boleta y factura de
  4 caracteres, nombre de almacén hasta 150, nombre de terminal hasta 120, número de serie hasta 120, impresora hasta
  100. Un test por schema fija esos límites para que no diverjan.
- `ipEquipo` se valida con formato de dirección IP en el formulario, porque la columna es `inet`.
- Los formularios del detalle no piden el padre: viene fijo por la ruta.
- Establecimiento se agrupa en cuatro secciones: Identificación (código, nombre, tipo, principal), Regulatorio
  (categoría regulatoria, anexo SUNAT, DIGEMID), Ubicación (dirección, ubigeo, referencia, latitud, longitud,
  teléfono, correo) y Operación (venta online, delivery, perfil de operación, zona horaria).
- En edición, el código de Establecimiento, Almacén y Terminal y el RUC de la empresa se muestran de solo lectura,
  porque los `PUT` no los reciben.

### Cambio de estado

- Empresa y Establecimiento: diálogo con un select de estados válidos que llama al `PATCH` de la fase 1.
- Almacén: el campo `activo` va dentro de su formulario de edición.
- Terminal: el campo `estado` (`ACTIVO | BLOQUEADO | MANTENIMIENTO`) va dentro de su formulario de edición.
- La insignia de estado usa `EstadoBadge` de `@boticas/ui-web`.

### Manejo de errores

Los errores llegan como `ApiError` con `problem`.

- `409`: se muestra el `detail` del problema dentro del modal (RUC o código duplicado).
- `404`: aviso de recurso no encontrado en la página de detalle.
- `400`: el modal muestra el error de validación del servidor.
- `403`: mensaje "No tienes permiso para esta acción".
- Cualquier otro error: mensaje genérico con opción de reintentar.

### Fuera de alcance

- Ocultar acciones según permisos del usuario.
- Eliminar registros (la API no lo permite).
- Enviar la asignación de rol desde estas pantallas.
- Handlers de MSW (modo mock) para los endpoints nuevos.
- Validar transiciones de estado en la UI.

## Orden de entrega

**Fase 1 — backend**, un commit por bloque:

1. Casos de uso, handlers, mapper y controller de cambio de estado, con tests unitarios al 100%.
2. Casos nuevos en `OrganizacionApiIntegrationTest`.
3. Actualización de `CLAUDE.md`.

**Fase 2 — frontend**, en rebanadas verticales con tests y commit propios:

1. Tipos, `api/` y schemas zod de las cuatro entidades.
2. Lista de empresas y detalle de empresa (formulario, cambio de estado y tabla de establecimientos).
3. Detalle de establecimiento (formulario en cuatro secciones y tablas de almacenes y terminales).
4. Integración con el resumen: enlaces en el árbol y navegación.

## Pruebas

- Backend: unitarias al 100% por clase y `OrganizacionApiIntegrationTest` contra PostgreSQL real; se cierra con
  `.\gradlew.bat check`.
- Frontend: cada archivo nuevo llega al 100% de líneas y ramas (umbral por archivo de Vitest), con el `apiClient`
  simulado como en `catalogo`. Se actualizan `OrganizationPage.test.tsx` y `feature-routes.test.ts`, que hoy asumen
  una sola ruta. Se cierra con `pnpm check`.
- Verificación final: se reconstruye la imagen Docker y se recorre el flujo en el navegador con una sesión real:
  crear empresa, editarla, suspenderla, agregar establecimiento, almacén y terminal, y revisar el árbol resultante.

## Riesgos

- **Límites duplicados** entre schemas zod y backend: si divergen, el servidor responde 400. Mitigación: tests que
  fijan los mismos valores.
- **Formato de IP:** un texto libre en `ipEquipo` llegaría a la columna `inet`. Mitigación: validación en el
  formulario.
- **Estado de `pnpm`:** `pnpm dev` quiso purgar `node_modules` y abortó por falta de TTY. Antes de correr
  `pnpm check` se verifica el estado de las dependencias y se usa `CI=true` si hace falta, sin reinstalar a ciegas.
- **Volumen de tests:** muchos archivos con cobertura 100%. Mitigación: rebanadas verticales con commit propio.
