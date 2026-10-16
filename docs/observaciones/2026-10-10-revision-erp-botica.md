# Observaciones de revisión del ERP Botica

Fecha: 10 de octubre de 2026.

Estado: hallazgos pendientes de corrección y validación.

## Alcance y límites

Se revisaron el frontend, el backend `service-botica` y las definiciones SQL de las migraciones, con foco en autenticación, autorización, ventas e integridad de inventario. Esta revisión no constituye una auditoría exhaustiva de todos los módulos.

Los hallazgos se sustentan en lectura del código y de las migraciones. Los escenarios descritos son casos para reproducir y validar; no se ejecutaron pruebas específicas de explotación ni escrituras de datos inconsistentes en PostgreSQL. No se inspeccionó una base de datos desplegada.

El repositorio ya contenía cambios locales. La revisión tomó como referencia ese estado de trabajo y no modificó código de aplicación ni migraciones.

## Resumen de hallazgos

| Identificador | Prioridad | Área | Observación |
| --- | --- | --- | --- |
| OBS-001 | Alta | Backend / seguridad | Los permisos efectivos pierden el ámbito organizacional de su asignación. |
| OBS-002 | Alta | Frontend / sesión | La caché de consultas persiste al cambiar de usuario. |
| OBS-003 | Alta | Frontend / autenticación | Falta el flujo de cambio obligatorio de contraseña. |
| OBS-004 | Alta | Backend / ventas | La venta no aplica las condiciones farmacéuticas presentes en el catálogo. |
| OBS-005 | Media | Base de datos / inventario | Las claves foráneas no garantizan la correspondencia entre SKU y lote. |
| OBS-006 | Media | Documentación | Los README describen un estado anterior al código actual. |

La prioridad expresa el impacto estimado para el proyecto, no una clasificación formal de vulnerabilidades.

## OBS-001 — Pérdida del ámbito organizacional de los permisos

### Evidencia

- [LocalAuthJdbcAdapter.java](../../service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/adapter/LocalAuthJdbcAdapter.java), método `findEffectivePermissions`: consulta los códigos de permiso por tenant, usuario, estado y vigencia. No conserva ni evalúa el ámbito de la asignación.
- [LocalJwtAuthenticationConverter.java](../../service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/configuration/LocalJwtAuthenticationConverter.java): convierte esos códigos en autoridades simples.
- [VentaController.java](../../service-botica/modules/ventas/src/main/java/com/softprimesolutions/ventas/api/controller/VentaController.java): utiliza `hasAuthority` para registrar, anular y consultar ventas.
- [RegistrarVentaHandler.java](../../service-botica/modules/ventas/src/main/java/com/softprimesolutions/ventas/application/usecase/command/RegistrarVentaHandler.java): valida terminal, turno, almacén y SKU dentro del tenant, pero no el ámbito asignado al actor.
- [V013 — Seguridad IAM](../cadena-farmacias-docs/database/migrations/V013__seguridad_iam.sql): `usuario_rol_ambito` sí modela ámbitos global, empresa, establecimiento, almacén y terminal.

### Impacto y escenario de validación

Un usuario con permiso de ventas asignado únicamente al establecimiento A puede superar la comprobación de autoridad para recursos del establecimiento B del mismo tenant. El aislamiento por tenant no resuelve esta restricción interna.

Validar con dos establecimientos del mismo tenant y un usuario cuyo rol esté limitado a uno de ellos. Intentar consultar una venta y registrar una operación en el otro establecimiento.

### Corrección propuesta

Evaluar el permiso junto con el ámbito del recurso solicitado en el backend. Resolver la relación entre venta, terminal, almacén, establecimiento y empresa; comprobarla contra las asignaciones vigentes del actor. Aplicar el control a consultas y operaciones de escritura.

### Criterio de cierre

Las operaciones fuera del ámbito autorizado se rechazan sin revelar datos ni modificar registros. Agregar pruebas que cubran permisos globales, ámbitos limitados y acceso entre establecimientos del mismo tenant.

## OBS-002 — Caché compartida entre sesiones sucesivas

### Evidencia

- [AuthSessionProvider.tsx](../../frontend/apps/erp-web/src/features/auth/model/AuthSessionProvider.tsx): `signOut` limpia tokens y estado de autenticación, pero no cancela consultas ni elimina su caché. La rama de fallo de renovación tampoco lo hace.
- [providers.tsx](../../frontend/apps/erp-web/src/app/providers.tsx): mantiene una instancia de `QueryClient` durante la vida del proveedor de la aplicación.
- [query-client.ts](../../frontend/apps/erp-web/src/app/query-client.ts): establece `staleTime: 30_000`.
- [ventas.api.ts](../../frontend/apps/erp-web/src/features/ventas/api/ventas.api.ts): las claves de consultas de ventas no incorporan tenant ni usuario.

### Impacto y escenario de validación

Tras consultar ventas con una cuenta, cerrar sesión e ingresar con otra en la misma pestaña, una consulta con la misma clave puede mostrar información de la sesión anterior. Dentro del tiempo de frescura puede reutilizarse sin una nueva petición inmediata; una entrada obsoleta también puede mostrarse durante la actualización.

### Corrección propuesta

Cancelar las consultas en curso y limpiar la caché de datos privados al cerrar o invalidar una sesión y al cambiar de identidad. Incorporar el contexto de tenant y, cuando corresponda, de usuario o autorización en las claves de consulta, o recrear el cliente para cada sesión.

### Criterio de cierre

Una prueba con dos cuentas consecutivas en la misma pestaña no muestra datos de la primera cuenta durante la carga de la segunda. Las respuestas de peticiones pendientes de la sesión anterior no repueblan la caché de la nueva sesión.

## OBS-003 — Cambio obligatorio de contraseña sin flujo de interfaz

### Evidencia

- [auth-tokens.types.ts](../../frontend/apps/erp-web/src/features/auth/api/auth-tokens.types.ts): declara `passwordChangeRequired` en la respuesta de autenticación.
- [AuthSessionProvider.tsx](../../frontend/apps/erp-web/src/features/auth/model/AuthSessionProvider.tsx): tanto el login como la renovación guardan los tokens y establecen el estado autenticado sin procesar ese indicador.
- [LoginPage.tsx](../../frontend/apps/erp-web/src/features/auth/pages/LoginPage.tsx): tras autenticar, navega al destino previo o al dashboard.
- [CredencialLocalForm.tsx](../../frontend/apps/erp-web/src/features/seguridad/components/CredencialLocalForm.tsx): propone `requireChange: true` al provisionar una credencial.
- [LocalJwtAuthenticationConverter.java](../../service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/configuration/LocalJwtAuthenticationConverter.java): cuando el token exige cambio de contraseña, solo concede `AUTH_PASSWORD_CHANGE`.
- [LocalAuthController.java](../../service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/controller/LocalAuthController.java): dispone del endpoint `/auth/password/change`.

### Impacto y escenario de validación

Un usuario con cambio obligatorio llega al panel con un token restringido. Las funciones protegidas por permisos de negocio pueden devolver 403, y no se encontró una pantalla frontend para completar el cambio. El problema afecta al alta de usuarios con la configuración predeterminada del formulario de credenciales.

### Corrección propuesta

Representar el estado de cambio obligatorio dentro de la sesión, dirigir al usuario a una pantalla específica y conectar el endpoint existente. Tras el cambio, completar la reautenticación o renovación conforme al contrato del backend antes de habilitar las rutas de negocio.

### Criterio de cierre

Una cuenta provisionada con cambio obligatorio puede iniciar sesión, cambiar su contraseña y acceder después a sus funciones autorizadas. No entra a las rutas de negocio mientras conserve el token restringido.

## OBS-004 — Condiciones farmacéuticas sin control en el registro de ventas

### Evidencia

- [V003 — Catálogo farmacéutico](../cadena-farmacias-docs/database/migrations/V003__catalogo_farmaceutico.sql): modela `condicion_venta.requiere_receta`, `requiere_retencion` y `clasificacion_controlada.requiere_receta_especial`; el SKU puede vincularse a un producto regulado.
- [ReferenciasVentasJdbcAdapter.java](../../service-botica/modules/ventas/src/main/java/com/softprimesolutions/ventas/infrastructure/persistence/write/adapter/ReferenciasVentasJdbcAdapter.java): la consulta de SKU obtiene descripción, unidad, fraccionamiento y actividad comercial, sin consultar las condiciones farmacéuticas.
- [RegistrarVentaHandler.java](../../service-botica/modules/ventas/src/main/java/com/softprimesolutions/ventas/application/usecase/command/RegistrarVentaHandler.java): `errorDeSku` comprueba existencia y operabilidad comercial; no exige la información de receta o dispensación.

### Impacto y escenario de validación

Según el flujo revisado, un SKU activo vinculado a un producto que exige receta puede tratarse como cualquier otro SKU activo al registrar una venta, si satisface las restantes validaciones de turno, almacén, pago y stock.

Esta observación se refiere a reglas ya representadas en el modelo del proyecto; no constituye una evaluación de cumplimiento legal.

### Corrección propuesta

Consultar y aplicar las condiciones del producto en el backend antes de confirmar la venta. Integrar los requisitos de receta, retención y dispensación según las reglas de negocio aprobadas. Mientras esos flujos no existan, rechazar explícitamente los productos que los necesiten.

### Criterio de cierre

Las pruebas diferencian productos de venta libre y productos con requisitos adicionales. Una petición directa al API no puede registrar una venta que omita los requisitos modelados, aunque el frontend no los solicite.

## OBS-005 — Integridad SKU-lote incompleta en las tablas de inventario

### Evidencia

En [V005 — Inventario, lotes y transferencias](../cadena-farmacias-docs/database/migrations/V005__inventario_lotes_transferencias.sql):

- `lote` contiene `tenant_id` y `sku_id`.
- `posicion_inventario` valida el SKU mediante `(tenant_id, sku_id)` y el lote mediante `(tenant_id, lote_id)`, por separado.
- `movimiento_inventario` utiliza el mismo patrón de referencias independientes.

[RegistrarMovimientoHandler.java](../../service-botica/modules/inventario/src/main/java/com/softprimesolutions/inventario/application/usecase/command/RegistrarMovimientoHandler.java) sí verifica la correspondencia con `mismoSku`. La brecha identificada está en la garantía del esquema, no en la ausencia de esa validación del servicio.

### Impacto y escenario de validación

Una escritura directa o una futura ruta que omita la comprobación del servicio puede combinar el SKU A con un lote del SKU B del mismo tenant sin infringir esas claves foráneas. Esto afecta a la consistencia del stock y a la trazabilidad.

### Corrección propuesta

Agregar una clave única referenciable en `lote` que incluya `(tenant_id, sku_id, id)` y claves foráneas compuestas desde las tablas que mantienen ambos identificadores. Revisar previamente los datos existentes y aplicar el ajuste mediante una nueva migración versionada.

### Criterio de cierre

Una prueba contra PostgreSQL demuestra que un lote del mismo tenant pero de otro SKU es rechazado por la base. Las combinaciones válidas y los flujos actuales de inventario siguen funcionando.

## OBS-006 — Documentación de estado desactualizada

### Evidencia

- [README principal](../../README.md), sección de estado actual: describe ventas, inventario y compras como módulos sin lógica, y el login frontend como simulado.
- [README del frontend](../../frontend/README.md), sección de implementación: describe varios módulos como placeholders y señala que no existe el endpoint de estructura corporativa.
- El código revisado contiene autenticación HTTP, controladores y casos de uso de negocio, pantallas conectadas y un `EstructuraCorporativaController`.

### Impacto

Las instrucciones de incorporación y planificación no reflejan el estado actual. Esto puede provocar trabajo duplicado o decisiones basadas en funcionalidades supuestamente inexistentes.

### Corrección propuesta

Actualizar los README con una matriz de capacidades que distinga implementación, integración y validación. Describir el modo mock y el modo HTTP según su configuración vigente. Evitar presentar la existencia de una pantalla o un controlador como prueba de que un módulo está terminado.

### Criterio de cierre

Cada afirmación sobre el estado de un módulo puede contrastarse con sus rutas, endpoints y pruebas; las capacidades pendientes quedan identificadas expresamente.

## Panorama del proyecto y las tablas

El backend declara 17 módulos de negocio en [settings.gradle](../../service-botica/settings.gradle). Se encontraron implementaciones de seguridad, organización, catálogo, inventario, compras y ventas. El frontend incluye flujos de autenticación, administración, POS y caja, entre otros; su existencia no implica cobertura funcional completa.

Flyway carga dos ubicaciones, configuradas en [application.yaml](../../service-botica/bootstrap-app/src/main/resources/application.yaml):

- `classpath:migrations`: V001–V018, provenientes de [database/migrations](../cadena-farmacias-docs/database/migrations), incorporadas como recursos por el build de `bootstrap-app`.
- `classpath:db/migration`: V019–V038, ubicadas en [bootstrap-app/src/main/resources/db/migration](../../service-botica/bootstrap-app/src/main/resources/db/migration).

Se contaron 116 declaraciones `CREATE TABLE` en esas 38 migraciones. Este es un inventario del código SQL, no un conteo consultado sobre una base desplegada; no incluye la tabla de historial que gestiona Flyway.

| Esquema | Tablas declaradas |
| --- | ---: |
| `sch_admin` | 1 |
| `sch_organizacion` | 8 |
| `sch_catalogo` | 14 |
| `sch_abastecimiento` | 7 |
| `sch_inventario` | 12 |
| `sch_precio` | 6 |
| `sch_venta` | 13 |
| `sch_dispensacion` | 9 |
| `sch_facturacion` | 3 |
| `sch_vigilancia` | 7 |
| `sch_finanzas` | 8 |
| `sch_seguridad` | 13 |
| `sch_auditoria` | 3 |
| `sch_integracion` | 5 |
| `sch_app` | 6 |
| Sin esquema explícito: `event_publication` | 1 |
| **Total** | **116** |

La tabla `event_publication` se crea sin calificar el esquema; su ubicación depende del esquema de ejecución. Las tablas de funcionalidades aún pendientes no deben confundirse con módulos funcionales terminados.

## Validación realizada

| Verificación | Resultado | Límite de la evidencia |
| --- | --- | --- |
| Backend: `gradlew.bat check --console=plain` | `BUILD SUCCESSFUL` | Gradle informó 97 tareas: 1 ejecutada, 2 recuperadas de caché y 94 al día. Se reutilizaron resultados anteriores; no se volvió a ejecutar toda la suite contra PostgreSQL. |
| Frontend: lint, dentro de `corepack pnpm check` | 0 errores y 4 advertencias | Dos advertencias por `watch` de React Hook Form y dos por exportaciones relacionadas con Fast Refresh. |
| Frontend: verificación de tipos | Aprobada | Pasaron `api-client`, `ui-web` y `erp-web`. |
| Frontend: `vitest run --coverage` | Sin resultado concluyente | La ejecución se detuvo después de varios minutos sin resultados. No se considera aprobada ni se atribuye una causa al retraso. |
| Frontend: `corepack pnpm build` separado | Sin resultado concluyente | Se detuvo antes de obtener resultado. |
| Pruebas de extremo a extremo y base desplegada | No verificadas | No se ejecutaron como parte de esta revisión. |

El resultado exitoso del comando de backend y las verificaciones de lint y tipos no descartan los problemas funcionales descritos. Cada corrección debe incorporar la validación específica indicada en su criterio de cierre.

## Orden recomendado de atención

1. Corregir la autorización por ámbito y el aislamiento de caché entre sesiones: OBS-001 y OBS-002.
2. Completar el cambio obligatorio de contraseña: OBS-003.
3. Restringir o implementar el tratamiento de productos con requisitos farmacéuticos: OBS-004.
4. Reforzar la integridad SKU-lote mediante una nueva migración: OBS-005.
5. Actualizar la documentación y completar las verificaciones pendientes: OBS-006, pruebas y build del frontend.
