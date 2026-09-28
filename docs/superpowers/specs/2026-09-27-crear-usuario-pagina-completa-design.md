# Diseño: página completa para crear usuario, con asignación de rol integrada

Fecha: 2026-09-27
Estado: propuesto

## Contexto

Hoy "Nuevo usuario" es un modal en `UsersPage.tsx` que solo pide datos personales (`UsuarioForm`: documento, nombres, correo, username, teléfono, MFA) vía `POST /api/v1/usuarios`. Asignar un rol a un usuario solo es posible **después** de creado, desde `UserDetailPage.tsx`, con el componente `AsignarRolDialog.tsx` (modal con `POST /usuarios/{userId}/role-assignments`).

Se pidió convertir el formulario de creación en una página completa que incluya, en el mismo flujo, "asignar rol" y "asignar sucursal". Investigación previa (fork de solo lectura) estableció los siguientes hechos, verificados contra el código y el DDL real:

1. **Backend: crear usuario y asignar rol son operaciones 100% independientes**, sin transacción compartida. `CrearUsuarioHandler.persist()` (`service-botica/modules/security/.../CrearUsuarioHandler.java:59-76`) hace su propio `writePort.save(identidad, user)`; `AsignarRolUsuarioHandler.persist()` (`AsignarRolUsuarioHandler.java:79-87`) hace su propio `writePort.save(assignment)` y además exige que el usuario **ya exista** (`writePort.userBelongsToTenant(...)`, línea 39). El frontend necesita 2 llamadas HTTP secuenciales: `POST /usuarios` → obtener `id` → `POST /usuarios/{id}/role-assignments`.

2. **"Asignar sucursal" no es una operación ni una tabla independiente.** Revisadas exhaustivamente las 13 tablas de `sch_seguridad` (`docs/cadena-farmacias-docs/database/migrations/V013__seguridad_iam.sql`): la única que relaciona un usuario (`membership_id`) con una sucursal es `usuario_rol_ambito` (líneas 252-296), y siempre lo hace junto con un `rol_id` en la misma fila — el `CHECK ck_seg_ura_scope` (líneas 280-286) fuerza que `establecimiento_id` solo pueda venir poblado cuando `tipo_ambito` incluye ese nivel. No existe `usuario_rol` ni `usuario_sucursal` separadas. `dispositivo_tienda` también tiene `establecimiento_id`, pero relaciona un dispositivo físico con su sucursal, no un usuario — no aplica. Conclusión: en este sistema, "asignar rol" y "asignar sucursal" son la misma operación de UI; no hay dos bloques independientes que agregar.

3. **`AsignarRolDialog.tsx` ya es reutilizable en su lógica interna** — no recibe ni usa `userId` dentro del formulario (`frontend/apps/erp-web/src/features/seguridad/components/AsignarRolDialog.tsx`); el `userId` se inyecta afuera, en el `mutationFn` del consumidor (`UserDetailPage.tsx:76-77`). El único acoplamiento a extraer es el `<Modal>` que envuelve el `<form>` (líneas 78 y 231 del archivo).

4. **No existe en el codebase ningún patrón de orquestar múltiples llamadas HTTP para "crear padre + hijos"** desde el frontend (búsqueda de `mutateAsync`/`await ...Mutation` en `features/`: cero resultados). Este sería el primer caso.

5. **No existe ningún precedente de página completa (con ruta propia) para "crear X"** en el proyecto — todas las creaciones actuales (usuarios, roles, catálogos de soporte) son modales. Esta spec introduce el primer caso de ese patrón, decisión consciente del usuario dado que el formulario resultante (datos personales + rol + ámbito con selects encadenados) es demasiado largo para un modal.

6. **Riesgo de estado a medias**: si `POST /usuarios` tiene éxito pero `POST .../role-assignments` falla después (rol no encontrado, ámbito inválido, red caída), el usuario queda creado y visible en el listado **sin rol asignado**, sin rollback automático posible (transacciones de BD separadas). Se decidió aceptar este riesgo con manejo explícito (ver sección "Manejo de fallo parcial").

## Alcance

1. Extraer el `<form>` interno de `AsignarRolDialog.tsx` a un componente nuevo y reutilizable `AsignacionRolForm.tsx` (sin `Modal`).
2. `AsignarRolDialog.tsx` pasa a ser un wrapper delgado (`<Modal><AsignacionRolForm .../></Modal>`) — comportamiento visible sin cambios para `UserDetailPage.tsx`, que sigue usándolo igual.
3. Nueva página `NewUserPage.tsx` en `seguridad/usuarios/nuevo`: datos personales (reutiliza `UsuarioForm` tal cual) + sección opcional "Asignar rol ahora" (toggle, colapsa `AsignacionRolForm`) + un solo botón "Crear usuario" que orquesta ambas llamadas.
4. `UsersPage.tsx`: el botón "Nuevo usuario" pasa de abrir un modal a ser un `<Link to="/seguridad/usuarios/nuevo">`; se elimina el modal de creación y su lógica de esa página.
5. Ruta nueva registrada en `features/seguridad/routes.tsx`, **antes** de `seguridad/usuarios/:userId` para no generar ambigüedad de matching.

**Explícitamente fuera de alcance:**
- Asignar múltiples roles/ámbitos en el mismo formulario de creación (una lista de asignaciones) — se descartó a favor de un solo bloque, consistente con que "asignar rol" ya es una operación de una sola asignación a la vez en `UserDetailPage`.
- Cualquier concepto de "sucursal" como campo o tabla independiente del rol — no existe en el modelo de datos, confirmado exhaustivamente.
- Transacción de backend que una creación de usuario y asignación de rol en una sola operación atómica — cambio de backend no solicitado; se maneja el riesgo desde el frontend.

## Diseño detallado

### `AsignacionRolForm.tsx` (nuevo, extraído)

Mismo contenido que el `<form>` actual de `AsignarRolDialog.tsx` (líneas 79-230): selects de rol, tipo de ámbito, empresa/establecimiento/almacén/terminal encadenados (via `corporateStructureQuery` de `features/organizacion`), vigencia opcional. Props: `tenantId`, `onSubmit`, `onCancel`, `isSubmitting?`, `errorMessage?` — idénticas a las actuales de `AsignarRolDialogProps` menos `open` (el control de visibilidad ya no es responsabilidad del form). Usa el mismo `asignacionRolSchema`/`AsignacionRolFormValues` ya existente en `schemas/asignacion-rol.schema.ts`, sin cambios.

### `AsignarRolDialog.tsx` (modificado, wrapper delgado)

```tsx
export function AsignarRolDialog({ open, tenantId, onSubmit, onCancel, isSubmitting, errorMessage }: AsignarRolDialogProps) {
  if (!open) return null;
  return (
    <Modal open={open} onClose={onCancel} title="Asignar rol">
      <AsignacionRolForm tenantId={tenantId} onSubmit={onSubmit} onCancel={onCancel} isSubmitting={isSubmitting} errorMessage={errorMessage} />
    </Modal>
  );
}
```

`UserDetailPage.tsx` no cambia — sigue consumiendo `AsignarRolDialog` exactamente igual.

### `NewUserPage.tsx` (nueva)

Estructura (usa `PageHeader` con breadcrumb "Seguridad / Usuarios / Nuevo usuario"):

1. **Sección "Datos del usuario"**: `UsuarioForm` tal cual existe (sin cambios a ese componente).
2. **Sección "Asignar rol"**: checkbox/toggle "Asignar rol ahora" (desmarcado por defecto). Al marcarse, muestra `AsignacionRolForm` embebido (sin su propio botón submit/cancelar — esos se controlan desde el nivel de página).
3. Un solo botón "Crear usuario" al pie de la página.

**Estado de la página**: dos `useState` para los valores de cada sub-formulario (o dos refs de `react-hook-form` si se opta por `useForm` a nivel de página con secciones), más `assignRoleEnabled: boolean`, `pendingRoleValues: AsignacionRolFormValues | null` (para el reintento tras fallo parcial), `createdUserId: string | null`, `phase: 'idle' | 'creating' | 'assigning' | 'partial-failure'`.

**Orquestación (`handleCreate`)**:
```
1. crearUsuario(apiClient, payload) 
   → onError: mostrar error inline en sección de datos personales, phase='idle', no continuar.
   → onSuccess(usuario):
      - createdUserId = usuario.id
      - si !assignRoleEnabled: navigate(`/seguridad/usuarios/${usuario.id}`)
      - si assignRoleEnabled:
          asignarRolUsuario(apiClient, usuario.id, roleValues)
            → onSuccess: navigate(`/seguridad/usuarios/${usuario.id}`)
            → onError: phase='partial-failure', guardar mensaje de ApiError
```

**Pantalla de fallo parcial** (cuando `phase==='partial-failure'`): banner "Usuario creado correctamente, pero no se pudo asignar el rol: `<mensaje>`" con dos acciones:
- Botón "Reintentar asignación" → vuelve a llamar `asignarRolUsuario(apiClient, createdUserId, pendingRoleValues)` sin tocar `crearUsuario` de nuevo.
- Enlace "Ir al detalle del usuario" → `navigate(`/seguridad/usuarios/${createdUserId}`)`, dejando la asignación de rol pendiente para hacerse después desde ahí (`AsignarRolDialog` ya existente en `UserDetailPage`).

### `UsersPage.tsx` (modificado)

- Se elimina: `createOpen`, `createError`, `createMutation`, el `<Modal>` de creación, el import de `UsuarioForm` y `crearUsuario`.
- El botón "Nuevo usuario" pasa a `<Link to="/seguridad/usuarios/nuevo">Nuevo usuario</Link>` (mismo patrón visual que el `Link` de "Ver detalle" ya usa `iconButtonClassName`/estilos de `Button` si aplica, o el propio componente `Button` de `ui-web` envolviendo un `Link`, según lo que ya exista en el sistema de diseño — verificar al implementar).

### Rutas (`features/seguridad/routes.tsx`)

```tsx
{
  path: 'seguridad/usuarios/nuevo',
  lazy: async () => {
    const { NewUserPage } = await import('./pages/NewUserPage');
    return { Component: NewUserPage };
  }
},
{
  path: 'seguridad/usuarios/:userId',
  // ... ya existente, sin cambios
}
```
El orden importa: `nuevo` debe declararse antes de `:userId` en el array.

## Manejo de errores

- Error al crear usuario (validación de dominio, duplicado de email/documento, etc.): `ApiError` inline en la sección de datos personales, mismo patrón ya usado en `UsersPage`/`RoleDetailPage` (mensaje del `ProblemDetail`, sin mapeo custom por campo).
- Error al asignar rol tras crear usuario exitosamente: pantalla de fallo parcial descrita arriba — es el único caso nuevo de manejo de error de esta spec, porque es el único escenario de "éxito parcial" posible en el flujo.
- Error de validación de campos (zod): igual que hoy, inline por campo en cada sub-formulario.

## Testing

**Frontend** (Vitest + Testing Library + MSW):
- `AsignacionRolForm.test.tsx` (nuevo): casos ya cubiertos hoy por `AsignarRolDialog.test.tsx` mueven su cobertura aquí (validación de campos, selects encadenados empresa→establecimiento→almacén/terminal, envío de valores) — sin `Modal` de por medio.
- `AsignarRolDialog.test.tsx` (existente): se simplifica a verificar que el wrapper renderiza el `Modal` + `AsignacionRolForm` correctamente cuando `open=true`, y nada cuando `open=false`. Debe seguir pasando sin regresión de comportamiento visible.
- `NewUserPage.test.tsx` (nuevo):
  1. Crear usuario sin rol (toggle desactivado) → solo `POST /usuarios`, navega a `/seguridad/usuarios/{id}`.
  2. Crear usuario con rol (toggle activado, rol+ámbito completos) → `POST /usuarios` seguido de `POST /usuarios/{id}/role-assignments`, navega a `/seguridad/usuarios/{id}`.
  3. Creación exitosa pero asignación de rol falla → banner de fallo parcial visible con el mensaje de error, botón "Reintentar asignación" reintenta solo el segundo POST, enlace "Ir al detalle" navega sin rol.
  4. Falla la creación del usuario (409/400) → error inline en datos personales, ninguna llamada de asignación de rol se dispara, no hay navegación.
- `UsersPage.test.tsx`: se reemplaza el test "crea un usuario nuevo y refresca el listado" (ya no aplica, el botón no abre modal) por un test que verifica que el link "Nuevo usuario" tiene `href="/seguridad/usuarios/nuevo"`.

Cobertura 100% líneas/ramas exigida por CLAUDE.md para todo archivo nuevo o reescrito (`AsignacionRolForm.tsx`, `NewUserPage.tsx`, `AsignarRolDialog.tsx` tras la reescritura, `UsersPage.tsx` tras la reescritura).

## Fuera de alcance

- Backend: ningún cambio. Se reutilizan `POST /usuarios` y `POST /usuarios/{userId}/role-assignments` exactamente como existen hoy.
- Asignación de múltiples roles/ámbitos en el mismo formulario de creación.
- Cualquier campo o tabla de "sucursal" independiente del rol.
- Edición de la asignación de rol ya creada desde esta misma página (se hace desde `UserDetailPage`, sin cambios).
