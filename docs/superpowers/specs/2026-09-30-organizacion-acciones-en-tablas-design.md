# Organización: columna «Acciones» en empresas y establecimientos — diseño

Fecha: 2026-09-30. Origen: observación de uso en el navegador real: en las tablas de Organización el nombre es un enlace al detalle, a diferencia del resto de la app.

## Contexto actual

- **Seguridad** (`UsersPage`, `RolesPage`): el nombre es texto plano y la columna «Acciones» lleva un icono de ojo («Ver detalle de …», un `Link` con `iconButtonClassName`).
- **Catálogo**, **almacenes** y **terminales POS**: la columna «Acciones» lleva un icono de lápiz («Editar …») que abre un modal de edición sin salir de la página.
- **Empresas** (`EmpresasPage`) y **establecimientos** (`EstablecimientosSection`): el nombre es el enlace al detalle y no hay columna «Acciones».

## Decisión

Se adopta el patrón de la app con **ambos iconos** (decidido por el usuario): ojo para ver el detalle y lápiz para editar en un modal sin navegar.

## Diseño

1. **Nombre como texto plano** en la tabla de empresas (razón social) y en la de establecimientos (nombre).
2. **Columna «Acciones»** en ambas tablas, con dos iconos del tamaño predeterminado (el mismo que Seguridad y Catálogo):
   - Ojo, `Ver detalle de {nombre}`: `Link` al detalle (`/organizacion/empresas/:id` y `/organizacion/establecimientos/:id`), con `aria-label` y `title`.
   - Lápiz, `Editar {nombre}`: abre el modal de edición sobre la tabla.
3. **Diálogos de edición compartidos.** Hoy el modal de edición, con su formulario, su `PUT`, el error del servidor y la invalidación de datos, vive dentro de `EmpresaDetailPage` y de `EstablecimientoDetailPage`. Se extrae a `EmpresaEditarDialog` y `EstablecimientoEditarDialog` en `features/organizacion/components/`, y los usan la lista y el detalle. No debe quedar lógica de edición duplicada.
4. **Alcance acotado.**
   - «Cambiar estado» sigue solo en el detalle.
   - El árbol del resumen de `/organizacion` conserva sus enlaces: no es una tabla de listado.
   - No se toca el backend.
   - Los datos antiguos inválidos (teléfono o sitio web) se corrigen en el modal igual que hoy en el detalle.

## Pruebas

- Tests unitarios con 100% de cobertura de líneas y ramas para los diálogos nuevos y las dos tablas.
- Se actualizan los tests y el e2e que hoy localizan el enlace por el nombre (`getByRole('link', { name: 'Boticas SAC' })`) para usar el ojo (`Ver detalle de Boticas SAC`).
- El e2e de Playwright en desktop, tablet y móvil entra al detalle con el ojo y edita desde la lista con el lápiz.

## Criterios de aceptación

1. En empresas y establecimientos el nombre no es un enlace y existe la columna «Acciones» con el ojo y el lápiz.
2. El ojo navega al detalle; el lápiz abre el modal de edición y, al guardar, la tabla se refresca sin cambiar de página.
3. El detalle sigue editando con el mismo diálogo, sin código de edición duplicado.
4. No hay regresiones en tests unitarios, e2e (tres viewports) ni gate de cobertura de los archivos nuevos.
