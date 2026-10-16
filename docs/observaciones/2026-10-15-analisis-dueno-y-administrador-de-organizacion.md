# Análisis: dueño del sistema, administrador de organización y usuarios de establecimiento

Fecha: 2026-10-15. Alcance: documentación vigente (`docs/cadena-farmacias-docs`), backend (`service-botica`), frontend (`frontend`) y datos reales de la BD local.

Método: lectura de documentación y código y consultas `SELECT` a la BD en Docker. **No se explotó ninguna vulnerabilidad**: los riesgos de la sección 3 se confirmaron leyendo código y comprobando que no existe la validación; no se probaron contra el sistema en ejecución.

## 1. Resumen

- El pedido es una jerarquía de tres niveles: **dueño del sistema** (`wilton.sullcaray.r@gmail.com`) → **administrador de la organización** → **usuarios de establecimiento** (cajeros por turno).
- **La documentación no define esa jerarquía.** Hay roles "candidatos", sin matriz de permisos, sin onboarding de clientes, sin reglas de delegación y sin turno laboral de usuario.
- **El sistema hoy no puede delegar de forma segura.** Un usuario con los permisos `seguridad.*` de gestión puede darse a sí mismo el rol `ADMIN` global y fijar la contraseña del dueño. Crear hoy un "administrador de organización" con esos permisos equivale a darle `ADMIN` global.
- Los permisos efectivos **ignoran el ámbito** de la asignación (excepto en ventas), así que un cajero asignado a un establecimiento recibe sus permisos como si fueran de todo el tenant.

Recomendación: no crear el administrador de organización hasta cerrar las reglas de la sección 5 (fase 1 y 2).

## 2. Qué dice la documentación

Rutas relativas a `docs/cadena-farmacias-docs/docs/`.

| Tema | Estado | Evidencia |
|---|---|---|
| Jerarquía tenant → empresa → establecimiento → almacén/terminal | Existe como modelo técnico (`TEC`); sin definición de negocio del "cliente" ni de su alta | `database/migrations/V001`, `V002`, `V013`; `01-negocio/03-mapa-capacidades.md` §A; `RF-ORG-001` mezcla "grupo/empresa" |
| Roles | Solo "candidatos, no normativos" | `08-seguridad/02-roles-permisos-ambitos.md` §2; `01-negocio/06-stakeholders-actores.md` §4 (`POR_VALIDAR`) |
| Alta de usuarios, delegación, límites de asignación de roles | **No existe** ningún RF, RN ni CU | solo `RF-SEC-002/003/004` genéricos; `RF-SEC-011` (segregación) "cuando se definan" |
| Onboarding de un tenant/cliente | **No existe** | el tenant FARMALAB y su `ADMIN` salen de un seed (`V022`) |
| Turno de caja | Definido | `RF-POS-001`, `RF-POS-018`, `RN-POS-001`, `04-dominio/04-estados-transiciones.md` §9 |
| Turno laboral del usuario / restricción horaria de acceso | **No existe** | `docs/observaciones/2026-10-10-revision-integral-modulos.md`: `rrhh` es solo estructura |
| Rol de plataforma entre tenants | **No existe** en el DDL: `rol.tenant_id` es obligatorio y `GLOBAL` significa "todo el tenant" | `V013` |
| RLS | Solo propuesta, no activa | `database/propuestas/rls_multitenancy.sql`; `06-datos/06` |

Contradicciones: el SRS junta "Administrador de plataforma/cadena" mientras `06-stakeholders` separa cadena, empresa e IAM; `RF-ORG-001` habla de "grupo/empresa" pero el DDL modela tenant ≠ empresa. No hay ADR sobre IAM, tenant ni roles de plataforma.

## 3. Estado real del sistema

### Datos hoy (BD local)

- 1 tenant (FARMALAB), 5 empresas, 4 establecimientos, 18 usuarios, 2 roles (`ADMIN`, `CAJERO`).
- `ADMIN`: GLOBAL, `es_sistema`, 68 permisos; lo tienen `wilton` y `e2e.inventario`.
- `CAJERO`: ESTABLECIMIENTO, 9 permisos (creado a mano, no sembrado); lo tienen los 12 usuarios de turno.
- Empresas y establecimientos son **todos de prueba** (`PRUEBA E2E`, `PRUEBA UI`, `PRUEBA CRITICA`, `Verificacion SAC`, `TamboSalud`). No hay datos de una organización real.

### Riesgos de seguridad en IAM (verificados por lectura de código/SQL)

| # | Severidad | Hallazgo | Dónde |
|---|---|---|---|
| 1 | Crítico | Los permisos efectivos son la unión de todas las asignaciones y **no miran `tipo_ambito`, empresa ni establecimiento**. El ámbito solo se valida en ventas (`AutorizacionAmbito`). | `LocalAuthJdbcAdapter.findEffectivePermissions`; `LocalJwtAuthenticationConverter` |
| 2 | Crítico | Asignar rol solo valida que usuario, rol y ámbito existan: se puede asignar `ADMIN` con ámbito `GLOBAL` a cualquiera, incluido uno mismo. | `AsignarRolUsuarioHandler`; `IamJpaWriteAdapter.resolveScope` |
| 3 | Crítico | `PUT /roles/{id}/permissions` no impide editar el rol de sistema `ADMIN` ni exige que el actor posea lo que concede. | `ReemplazarPermisosRolHandler` |
| 4 | Crítico | `POST /usuarios/{id}/credencial-local` permite fijar la contraseña de cualquier usuario del tenant, incluido el dueño. Igual con estado, revocación de roles y sesiones. | `LocalAuthController`, `SecurityControlController` |
| 5 | Alto | `security` toma `tenantId` del parámetro/body y **no lo compara con el `tid` del JWT**. Inocuo con un tenant; explotable con dos. | `UsuarioController`, `RolController`, `SecurityControlController` |
| 6 | Alto | `CrearRolRequest` acepta `systemRole` desde el cliente. | `IamApiMapper` |
| 7 | Alto | El listado de usuarios devuelve todo el tenant, sin filtro por empresa. | `IamJdbcReadRepository` |
| 8 | Medio | Organización, inventario, compras y catálogo solo usan `hasAuthority`; sin ámbito de empresa/establecimiento. | controladores de cada módulo |
| 9 | Medio | RLS no está activo (0 políticas); el aislamiento es solo por filtros de consulta. | `pg_policies` vacío |

### Frontend

- **Sin control de acceso en la UI:** el menú lateral es fijo (un cajero ve Seguridad y Organización) y el router solo autentica; no hay guardas por permiso ni botones condicionados. El backend responde 403 y la UI muestra un error genérico.
- El front **no conoce los permisos** del usuario. `GET /auth/me` (en desarrollo) trae nombre y roles/ámbitos, no permisos.
- **El rol `CAJERO` actual no puede operar:** Caja y POS cargan establecimientos y terminales desde `/estructura-corporativa` y `/organizacion/terminales-pos`, que exigen `organizacion.empresas.consultar` y `organizacion.terminales-pos.consultar`. Dárselos tal cual expondría **todas** las empresas del tenant; falta un endpoint "mi puesto de trabajo" filtrado por ámbito.
- Estado de features: seguridad, organización, catálogo, inventario, compras, ventas, POS y caja están conectadas al backend real; **clientes** es placeholder; **dashboard** pide `/dashboard/summary`, que no existe en el backend.
- El alta de usuario son tres pasos (crear → asignar rol → fijar contraseña) y ofrece todos los roles y ámbitos del tenant, incluido `GLOBAL`.

### Otros hallazgos

- `nombre_mostrar` queda vacío al crear usuarios (la lista muestra el correo): `CrearUsuarioHandler` no lo deriva de nombres y apellidos y el formulario lo deja en blanco. 16 de 18 memberships lo tienen vacío.
- Auditoría poco legible: `created_by`/`granted_by` guardan el UUID del actor, no un nombre.
- La navegación dinámica por permisos (`sch_app.menu_navegacion`, V016) existe en la BD y en ADR-010, pero ningún código la consume.

## 4. Modelo propuesto

| Nivel | Rol | Ámbito | Puede |
|---|---|---|---|
| Dueño del sistema | `OWNER` (plataforma) | GLOBAL | Crear/suspender organizaciones, administrar catálogos globales y crear administradores de organización. No opera ventas ni dispensación. |
| Administrador de organización | `ADMIN_ORGANIZACION` (`es_sistema`) | EMPRESA | Crear usuarios, asignar roles y administrar establecimientos **solo dentro de su empresa**. |
| Usuarios de establecimiento | `CAJERO`, jefe de tienda, almacén, químico… | ESTABLECIMIENTO / TERMINAL | Operar su establecimiento. |

Reglas mínimas que el backend debe imponer (todas hoy ausentes):

1. El `tenantId` sale siempre del JWT.
2. Un actor solo asigna ámbitos contenidos en los suyos; `GLOBAL` solo lo asigna un actor `GLOBAL`.
3. Un actor solo concede permisos que él posee; los roles `es_sistema` o `GLOBAL` solo los toca el `OWNER`.
4. Contraseña, estado, sesiones y revocación solo sobre usuarios dentro de su ámbito y de rango inferior.
5. El listado de usuarios se filtra por el ámbito del actor.
6. El cliente no puede marcar un rol como `systemRole`.

## 5. Plan por fases (tamaños estimados)

| Fase | Contenido | Tamaño |
|---|---|---|
| 0 | Decisiones del dueño del producto (sección 6) y ADR de IAM/tenant | S |
| 1 | `tid` del JWT en todo `security`; rechazar edición de roles de sistema; ignorar `systemRole` del cliente | S |
| 2 | Resolver el ámbito del actor y validar asignación de roles, concesión de permisos y operaciones sobre usuarios; filtrar listado de usuarios; tests negativos por login real | M |
| 3 | Roles `OWNER` y `ADMIN_ORGANIZACION` por seed; separar el dueño del `ADMIN` global actual | S |
| 4 | Endpoint "mi puesto de trabajo" (establecimientos/terminales del ámbito); permisos al cajero; `permissions` en `/auth/me` | M |
| 5 | Frontend: menú y rutas por permiso, página 403, botones condicionados, alta de usuario en un solo asistente con plantilla de turno mañana/tarde y selector acotado al ámbito | M-L |
| 6 | Ámbito en organización, inventario, compras y catálogo; activar RLS como defensa en profundidad | L |
| 7 | `nombre_mostrar` derivado, auditoría legible, limpieza de datos de prueba | S |

## 6. Decisiones que necesita el dueño del producto

1. ¿El administrador de organización administra **una empresa (RUC)** o **un grupo de empresas** del mismo cliente? (cambia el ámbito `EMPRESA` vs un nivel "grupo").
2. ¿El dueño del sistema debe seguir siendo `ADMIN` del tenant o pasar a un rol de plataforma separado, y quién crea al cliente nuevo (alta de organización)?
3. ¿Habrá un solo tenant para todos los clientes o un tenant por cliente? Hoy hay uno y el aislamiento entre clientes depende de filtros.
4. ¿El turno mañana/tarde es solo organizativo (convención de nombre) o debe **restringir el acceso por horario**? No hay concepto de turno laboral en el modelo.
5. ¿Un administrador de organización puede crear roles propios o solo asignar los roles estándar?
6. Qué datos de prueba se conservan antes de entregar el sistema.
