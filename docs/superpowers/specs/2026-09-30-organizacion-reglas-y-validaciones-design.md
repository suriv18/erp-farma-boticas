# Organización: reglas de negocio y validaciones — diseño

Fecha: 2026-09-30. Origen: revisión crítica del flujo de alta y edición de empresa, establecimiento, almacén y caja (terminal POS) hecha en el navegador real contra el backend.

## Alcance

Corrige los nueve hallazgos de mayor riesgo de esa revisión. Se divide en dos planes independientes:

- **Plan A, backend (`service-botica/modules/organizacion`):** reglas de negocio que hoy no se aplican.
- **Plan B, frontend (`frontend/apps/erp-web/src/features/organizacion`):** validaciones, confirmaciones y avisos.

El plan A se implementa primero, porque la interfaz depende de sus reglas y mensajes.

Fuera de alcance: accesibilidad de modales (Escape, foco), nomenclatura («cajas» frente a «terminales»), listados con más columnas y el resto de hallazgos medios y bajos de la revisión.

## Decisiones tomadas

| Tema | Decisión | Clase de evidencia |
|---|---|---|
| Ámbito de unicidad de las series de comprobantes | Por empresa (RUC): dos cajas de la misma empresa no pueden repetir una serie, aunque estén en locales distintos | POR_VALIDAR con contabilidad |
| Altas con el padre no operativo | Se bloquean según el estado del padre (detalle en A3) | POR_VALIDAR |
| Dígito verificador del RUC | Se valida en frontend y backend, solo en el alta | NORM (algoritmo módulo 11 de SUNAT) |
| Almacén `REFRIGERADO` | Debe controlar temperatura | POR_VALIDAR |

## Plan A — Backend

### A1. Series únicas por empresa

- Al crear o editar una caja se comprueba que su serie de boleta y su serie de factura no estén activas en otra caja de la misma empresa. La caja que se edita se excluye de la comprobación.
- La comprobación previa vive en `OrganizacionJpaWriteAdapter`, con el mismo patrón que `DUPLICATE_CODIGO`. Se añaden los resultados `DUPLICATE_SERIE_BOLETA` y `DUPLICATE_SERIE_FACTURA` a `SaveTerminalOutcome`.
- La base de datos lo refuerza con una migración nueva `V026` en `bootstrap-app/src/main/resources/db/migration`: dos índices únicos parciales sobre `(tenant_id, empresa_id, serie_boleta_defecto)` y `(tenant_id, empresa_id, serie_factura_defecto)`, con `es_activo = '1'` y serie no nula.
- Respuesta `409` con los códigos `ORG_TERMINAL_SERIE_BOLETA_DUPLICADA` y `ORG_TERMINAL_SERIE_FACTURA_DUPLICADA`. Mensaje: «La serie B001 ya está asignada a otra caja de esta empresa.»

### A2. Dígito verificador del RUC

- Validación módulo 11 en `EmpresaOperadora.create`, en el dominio y sin depender de Spring.
- Se mantiene la regla de formato actual (11 dígitos, empieza con 10 o 20).
- Mensaje: «El RUC no es válido: el dígito verificador no coincide.»
- Solo aplica al alta: el RUC no se edita.

### A3. Altas según el estado del padre

Los enums `EstadoEmpresaOperadora` y `EstadoEstablecimiento` ganan el método `admiteAltasDeHijos()`:

| Padre | Estado | ¿Admite altas de hijos? |
|---|---|---|
| Empresa | `ACTIVO` | Sí |
| Empresa | `SUSPENDIDO`, `BLOQUEADO` | No (establecimientos nuevos) |
| Establecimiento | `ACTIVO`, `REMODELACION` | Sí |
| Establecimiento | `SUSPENDIDO`, `CLAUSURADO` | No (almacenes y cajas nuevos) |

- El adaptador consulta el estado del padre al crear un hijo y devuelve `409`. Se añaden los resultados `EMPRESA_NO_OPERATIVA` y `ESTABLECIMIENTO_NO_OPERATIVO`.
- Mensaje: «El establecimiento no está operativo (suspendido o clausurado); no admite almacenes nuevos.» (sin nombrar el estado concreto).
- Editar elementos ya existentes sigue permitido.

### A4. Almacén coherente

Validación en `Almacen.create` y `Almacen.updateDetails`, con mensajes claros:

- Si se indican ambas temperaturas, la mínima no puede ser mayor que la máxima. La restricción `ck_almacen_temperatura` de la base de datos ya existe, pero hoy produce un error genérico.
- Si `controlTemperatura` es verdadero, ambas temperaturas son obligatorias.
- Si el tipo es `REFRIGERADO`, `controlTemperatura` debe ser verdadero.

### Pruebas del plan A

- Tests unitarios de dominio y de handlers, con el gate de JaCoCo al 100% para cada clase nueva o modificada.
- Casos nuevos en `OrganizacionApiIntegrationTest` (Testcontainers) para cada `409` y `400`, incluida la migración `V026`.
- `gradlew check` completo (ArchUnit y Spring Modulith) debe pasar.

### Riesgo de datos

El índice único de `V026` fallará si existen series duplicadas en la base. Hoy existe una: la caja de prueba `CRIT-POS2` repite las series de `CRIT-POS` en el mismo establecimiento. Esa caja debe eliminarse, con confirmación explícita del usuario, antes de aplicar la migración.

## Plan B — Frontend

### B1. RUC

- Utilidad nueva `lib/ruc.ts` con el algoritmo módulo 11.
- `empresa.schema` la usa con el mismo mensaje que el backend.
- Los RUC de tests y fixtures (`20123456789`, `20999999992`) se sustituyen por RUC válidos.

### B2. Teléfono y sitio web

- Teléfono: solo dígitos, espacios, `+`, `-` y paréntesis, entre 6 y 15 caracteres.
- Sitio web: URL `http(s)` válida.
- Aplica a empresa y establecimiento.
- En los detalles, correo, sitio web y teléfono se muestran como enlaces `mailto:`, `https:` y `tel:`.

### B3. Cambio de estado con confirmación

- `CambiarEstadoDialog` recibe, por estado, un texto opcional de consecuencias.
- Para `SUSPENDIDO`, `BLOQUEADO` y `CLAUSURADO` muestra un aviso con lo que implica y exige marcar «Entiendo las consecuencias» antes de habilitar el botón de guardar.
- El botón se deshabilita si el estado elegido es el actual.
- El título nombra el elemento afectado, por ejemplo «Cambiar estado de Botica Crítica Norte».

### B4. Almacén coherente

- Al elegir `REFRIGERADO`, se marca «Controla temperatura».
- Los campos de temperatura solo son editables si esa casilla está marcada.
- `almacen.schema` replica las reglas de A4, con el error asociado al campo correspondiente.

### B5. Series en mayúscula

Las series de boleta y factura se convierten a mayúscula al escribir y se recortan los espacios.

### B6. Padre no operativo

- La página de detalle muestra un aviso superior cuando el padre no admite altas, con la misma tabla de A3.
- Los botones «Nuevo …» correspondientes se deshabilitan y explican el motivo.
- Es un espejo de A3: el servidor sigue siendo la autoridad.

### Pruebas del plan B

- Tests unitarios con 100% de cobertura de líneas y ramas por cada archivo nuevo.
- Spec de Playwright en desktop, tablet y móvil que cubra: RUC inválido, serie duplicada (`409` simulado con `page.route`), confirmación de cambio de estado, marcado automático de refrigerado y botones deshabilitados con padre no operativo.
- Verificación final en el navegador real contra el backend ya actualizado.

## Criterios de aceptación

1. No se puede crear una empresa con RUC de dígito verificador inválido, ni desde el formulario ni llamando a la API.
2. Dos cajas activas de una misma empresa no pueden compartir serie de boleta ni de factura.
3. Un padre `SUSPENDIDO`, `BLOQUEADO` o `CLAUSURADO` (según A3) rechaza altas de hijos con un mensaje claro, y la interfaz lo anticipa.
4. Un almacén con temperaturas incoherentes, o refrigerado sin control de temperatura, se rechaza con un mensaje que nombra el problema.
5. Cambiar a `SUSPENDIDO`, `BLOQUEADO` o `CLAUSURADO` requiere confirmación explícita.
6. Teléfono y sitio web inválidos se rechazan en el formulario.
