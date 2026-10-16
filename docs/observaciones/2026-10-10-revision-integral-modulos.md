# Revisión integral de módulos del ERP Botica

Fecha: 10 de octubre de 2026. Estado: revisión funcional y técnica estática; correcciones pendientes.

## 1. Dictamen

El proyecto tiene un núcleo transaccional parcial de ERP/POS: seguridad, organización, catálogo, compras, inventario y ventas contienen implementación. No está acreditado como una solución completa para operar una cadena comparable a las referencias comerciales estudiadas.

De los 17 módulos de negocio declarados, seis contienen clases Java de implementación y once contienen únicamente estructura de paquetes y configuración. Hay 30 archivos `*Controller.java` en los seis módulos activos. Este conteo describe presencia de código, no porcentaje de avance, calidad o cobertura funcional.

La arquitectura y el modelo de datos abarcan más capacidades que la aplicación ejecutable. Las brechas decisivas se encuentran en autorización por ámbito, dispensación, valoración, impuestos y facturación, clientes, precios/promociones, medios de pago, logística, omnicanalidad y operación verificable.

Documentos relacionados:

- [Hallazgos iniciales OBS-001–OBS-006](2026-10-10-revision-erp-botica.md).
- [Comparativa con cadenas y plataformas del mercado](2026-10-10-comparativa-mercado.md).
- [Inventario completo de las 116 tablas](2026-10-10-inventario-tablas.md).
- [Análisis de producto, SKU y las 14 tablas del catálogo; OBS-020–023](2026-10-10-analisis-modelo-catalogo.md).

## 2. Método y alcance efectivo

Se inventariaron los 17 módulos, las 12 funcionalidades frontend, los componentes compartidos, las migraciones de creación y las modificaciones posteriores. En los módulos implementados se siguieron rutas, controladores, contratos, casos de uso, modelos y escrituras relevantes. Se contrastaron los procesos de compra → recepción → inventario → venta → anulación → caja y los controles transversales de sesión y autorización.

Se inspeccionaron pruebas existentes y sus mecanismos de simulación. No se ejecutó nuevamente una suite completa en esta ampliación. Los resultados de comandos de la revisión anterior se conservan como evidencia histórica, no como una nueva validación de todos los hallazgos.

No se inspeccionó una base desplegada, no se realizaron compras en sitios de terceros, no se accedió a sistemas internos de cadenas y no se modificó código de aplicación. La revisión cubre todos los módulos a nivel de inventario y capacidades, pero no afirma haber verificado todas las ramas de los cientos de clases ni todas las combinaciones de datos en ejecución.

Clasificación utilizada:

| Estado | Significado |
| --- | --- |
| Implementación parcial | Hay lógica y persistencia para un subconjunto de procesos. No significa listo para producción. |
| Estructura | Solo paquetes/configuración; sin clases de negocio Java distintas de `package-info.java`. |
| Diseño SQL | Hay tablas, pero no se encontró un flujo completo que opere la capacidad. |
| Placeholder | Pantalla descriptiva sin proceso funcional. |
| No acreditado | Requiere pruebas, despliegue o información no disponible. |

## 3. Revisión de los 17 módulos de negocio

Los conteos excluyen `package-info.java`. La columna de pruebas cuenta archivos terminados en `Test.java` dentro de `src/test` del módulo; no incluye pruebas centralizadas en `bootstrap-app`, no equivale al número de casos y no expresa cobertura.

| Módulo | Java | Controladores | Archivos de pruebas | Estado |
| --- | ---: | ---: | ---: | --- |
| `security` | 117 | 5 | 13 | Implementación parcial |
| `organizacion` | 132 | 5 | 39 | Implementación parcial |
| `catalogo` | 297 | 12 | 38 | Implementación parcial |
| `compras` | 99 | 3 | 27 | Implementación parcial |
| `inventario` | 78 | 3 | 23 | Implementación parcial |
| `ventas` | 84 | 2 | 32 | Implementación parcial |
| `farmacia` | 0 | 0 | 0 | Estructura |
| `clientes` | 0 | 0 | 0 | Estructura |
| `finanzas` | 0 | 0 | 0 | Estructura |
| `rrhh` | 0 | 0 | 0 | Estructura |
| `crm` | 0 | 0 | 0 | Estructura |
| `cmr` | 0 | 0 | 0 | Estructura |
| `app-channel` | 0 | 0 | 0 | Estructura |
| `logistica` | 0 | 0 | 0 | Estructura |
| `pagos` | 0 | 0 | 0 | Estructura |
| `notificaciones` | 0 | 0 | 0 | Estructura |
| `integraciones` | 0 | 0 | 0 | Estructura |

Fuente del universo: [settings.gradle](../../service-botica/settings.gradle) y [modules](../../service-botica/modules).

### 3.1 Seguridad — `security`

**Implementado:** login local, emisión y renovación de tokens, rotación/revocación, cambio y recuperación de contraseña en API, administración de usuarios, roles, permisos, asignaciones, sesiones y dispositivos. El token se comprueba contra el estado de sesión y las autoridades se resuelven desde persistencia.

**Brechas:** OBS-001–003 del informe inicial y OBS-013 de este informe. El ámbito de la asignación no acompaña al permiso efectivo; faltan aislamiento de caché y flujo frontend de contraseña obligatoria. La recuperación publica un evento, pero no se encontró un consumidor que entregue el token al usuario.

**Tablas:** `sch_seguridad`; el diseño también utiliza navegación en `sch_app`. Tener estados y fechas de vigencia no basta para garantizar autorización de recursos.

**Evidencia:** [controladores](../../service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/controller), [LocalAuthService](../../service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/usecase/command/LocalAuthService.java).

### 3.2 Organización — `organizacion`

**Implementado:** empresas, establecimientos, almacenes, terminales y estructura corporativa; códigos, estados y referencias organizacionales. Hay restricciones de series de terminal en migraciones posteriores.

**Brechas:** no se encontraron flujos equivalentes para autorizaciones sanitarias, profesionales/asignaciones y ubicaciones detalladas, aunque sus tablas existen. Cambiar el estado del establecimiento no impide por sí solo la venta desde terminales aún activas: OBS-009. Las propiedades de venta online o Store Edge no acreditan esas funciones.

**Tablas:** ocho en `sch_organizacion`, con implementación concentrada en empresa, establecimiento, almacén y terminal.

**Evidencia:** [controladores](../../service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/controller), [adaptador de escritura](../../service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/write/adapter/OrganizacionJpaWriteAdapter.java).

### 3.3 Catálogo — `catalogo`

**Implementado:** producto regulado separado del SKU comercial; principios activos y asociaciones; códigos de barras; categorías, marcas y rubros; catálogos de condición de venta, forma, vía, clasificación controlada, unidad y documento. El SKU incluye fraccionamiento, requisitos de lote/vencimiento, afectación de IGV y precio de referencia.

**Brechas:** varios atributos quedan almacenados sin gobernar los procesos posteriores: OBS-008, OBS-010, OBS-011 y OBS-015. El precio de referencia no constituye un motor de precios: OBS-016. No se encontró integración operativa con registro sanitario ni motor de admisibilidad de surtido por tipo de establecimiento.

**Tablas:** 14 en `sch_catalogo`. Los catálogos globales deben distinguirse de las entidades con tenant al diseñar permisos y actualizaciones.

**Evidencia:** [SKUComercial](../../service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/SKUComercial.java), [controladores](../../service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller).

### 3.4 Compras — `compras`

**Implementado:** proveedores, órdenes con líneas, aprobación/emisión/anulación, recepción con aceptación/rechazo e ingreso a inventario. Hay comprobación de almacén de destino, recepción sobre orden bloqueada e idempotencia si se suministra la clave. Se registran temperatura y humedad de recepción.

**Brechas:** el costo de recepción no viaja al inventario: OBS-007. La recepción exige lote/vencimiento de forma general: OBS-010. No se encontró un flujo completo de solicitudes de compra, reposición automática, cotejo factura–orden–recepción, cuenta por pagar ni devolución a proveedor. Capturar temperatura no equivale a evaluar límites del producto y gestionar una cuarentena sanitaria.

**Tablas:** siete en `sch_abastecimiento`; solicitudes y líneas de solicitud permanecen como diseño respecto de los endpoints revisados.

**Evidencia:** [RegistrarRecepcionHandler](../../service-botica/modules/compras/src/main/java/com/softprimesolutions/compras/application/usecase/command/RegistrarRecepcionHandler.java), [Recepcion](../../service-botica/modules/compras/src/main/java/com/softprimesolutions/compras/domain/model/Recepcion.java), [controladores](../../service-botica/modules/compras/src/main/java/com/softprimesolutions/compras/api/controller).

### 3.5 Inventario — `inventario`

**Implementado:** consulta de posiciones/lotes, movimientos, bloqueo/desbloqueo de lote, ingreso desde compras, salida FEFO para venta y reintegro por anulación. Usa bloqueos de posiciones, control de versión, cantidades reservadas e idempotencia. El adaptador transaccional marca rollback cuando el caso de uso devuelve fallo.

**Brechas:** no se encontró ciclo operativo de reservas, transferencias con despacho/recepción, conteos y conciliación, reposición ni recall completo. Registrar movimientos no sustituye esos procesos. Ver integridad SKU-lote (OBS-005), costos (OBS-007), productos sin lote (OBS-010) y día de vencimiento (OBS-012). El modelo permite ubicaciones, pero las consultas del flujo revisado restringen posiciones a `ubicacion_id IS NULL`.

**Tablas:** 12 en `sch_inventario`; el código se concentra en lote, posición y movimiento.

**Evidencia:** [RegistrarSalidaVentaHandler](../../service-botica/modules/inventario/src/main/java/com/softprimesolutions/inventario/application/usecase/command/RegistrarSalidaVentaHandler.java), [InventarioJdbcWriteAdapter](../../service-botica/modules/inventario/src/main/java/com/softprimesolutions/inventario/infrastructure/persistence/write/adapter/InventarioJdbcWriteAdapter.java).

### 3.6 Ventas — `ventas`

**Implementado:** apertura/cierre de turno, venta en efectivo con vuelto, consulta de ventas, numeración de operación, consumo FEFO y anulación con reintegro mientras el turno está abierto. Se conserva un comprobante interno imprimible, identificado expresamente como no fiscal.

**Brechas:** no hay facturación electrónica operativa, impuestos calculados, motor de promociones, fidelización, selección de cliente funcional, pagos combinados ni ciclo de devoluciones posterior al cierre. La anulación total de una venta con turno abierto no equivale a devolución parcial, reembolso y nota de crédito. Ver OBS-008, OBS-009, OBS-011, OBS-015 y OBS-016.

**Tablas:** parte de las 13 de `sch_venta`; clientes, consentimientos y devoluciones tienen DDL sin proceso comercial completo. `sch_facturacion` aporta diseño adicional, no emisión operativa.

**Evidencia:** [Venta](../../service-botica/modules/ventas/src/main/java/com/softprimesolutions/ventas/domain/model/Venta.java), [AnulacionVenta](../../service-botica/modules/ventas/src/main/java/com/softprimesolutions/ventas/domain/model/AnulacionVenta.java), [ComprobanteVenta](../../frontend/apps/erp-web/src/features/ventas/components/ComprobanteVenta.tsx).

### 3.7 Farmacia — `farmacia`

Solo estructura en [el módulo](../../service-botica/modules/farmacia). No se encontraron casos de uso para recepción/validación de receta, dispensación, autorización profesional, receta especial, registro de controlados o farmacovigilancia. `sch_dispensacion` y `sch_vigilancia` contienen tablas para esos conceptos. Prioridad alta antes de habilitar el surtido que requiera esos controles.

### 3.8 Clientes — `clientes`

Solo estructura en [el módulo](../../service-botica/modules/clientes). `CustomersPage` es un placeholder. Hay tablas `cliente` y `cliente_consentimiento` en `sch_venta`, pero no un proceso frontend/API de ficha, identificación, historial y consentimiento conectado a ventas. Es dependencia de CRM, fidelización y omnicanalidad.

### 3.9 Finanzas — `finanzas`

Solo estructura en [el módulo](../../service-botica/modules/finanzas). Las ocho tablas de `sch_finanzas` incluyen cuentas bancarias, factura de proveedor, cuentas por pagar/cobrar, pagos, posting y reporte de precios. No se encontraron servicios para conciliación, vencimientos, asientos, cuentas o emisión del reporte. El cierre de caja de ventas no acredita un ERP financiero.

### 3.10 Recursos humanos — `rrhh`

Solo estructura en [el módulo](../../service-botica/modules/rrhh). No se encontró nómina, asistencia, turnos laborales o expedientes. Las tablas organizacionales de profesionales no sustituyen esos procesos. Puede resolverse por integración si el alcance del producto no incluye RR. HH.; es una decisión de producto, no un requisito deducible del sitio web de una cadena.

### 3.11 CRM — `crm`

Solo estructura en [el módulo](../../service-botica/modules/crm), con dependencia declarada hacia clientes. No se encontró segmentación, campañas, historial de contacto o automatización comercial. Depende de un modelo operativo de cliente y de reglas de consentimiento.

### 3.12 CMR — `cmr`

Solo estructura en [el módulo](../../service-botica/modules/cmr), también dependiente de clientes. La anotación lo identifica como CMR, pero no ofrece casos de uso que permitan establecer su responsabilidad real. Debe aclararse su alcance y su diferencia con CRM; no se interpreta automáticamente como monedero, fidelización ni producto financiero.

### 3.13 Canal digital — `app-channel`

Solo estructura en [el módulo](../../service-botica/modules/app-channel). No se encontró tienda B2C, checkout, pedido omnicanal, entrega/recojo ni cuenta de consumidor. El frontend actual es backoffice/POS para personal; su login no constituye una cuenta de comprador final.

### 3.14 Logística — `logistica`

Solo estructura en [el módulo](../../service-botica/modules/logistica). No se encontraron asignación de pedidos, picking/packing, rutas, entrega, prueba de entrega o incidencias. El diseño SQL de transferencias de inventario tampoco acredita su ejecución. Definir frontera entre logística de tienda, transferencias y delivery.

### 3.15 Pagos — `pagos`

Solo estructura en [el módulo](../../service-botica/modules/pagos). El pago en efectivo sí existe, implementado directamente en ventas. No se encontró autorización/captura con proveedor, tarjeta, billetera, pago dividido, conciliación ni reversa externa. El catálogo `medio_pago` y sus filas no equivalen a adaptadores operativos.

### 3.16 Notificaciones — `notificaciones`

Solo estructura en [el módulo](../../service-botica/modules/notificaciones). Existen tablas de plantilla/notificación en `sch_app`, pero no se encontraron consumidores ni entrega por correo/SMS. La recuperación de contraseña ejemplifica una dependencia pendiente: OBS-013. También faltan alertas operativas y mensajes de estado de pedido.

### 3.17 Integraciones — `integraciones`

Solo estructura en [el módulo](../../service-botica/modules/integraciones). `sch_integracion` define outbox/inbox, checkpoint y reintentos; no se encontraron publicadores/consumidores de negocio que completen esos ciclos. No se acreditan conectores SUNAT/PSE/OSE, DIGEMID, ERP externo o sincronización offline. La infraestructura Spring Modulith no constituye por sí sola una integración funcional.

## 4. Revisión de las 12 funcionalidades frontend

Fuente: [features](../../frontend/apps/erp-web/src/features). La valoración se basa en rutas, páginas, clientes API y contratos, no en una ejecución completa de cada pantalla.

| Funcionalidad | Evidencia de implementación | Limitación principal |
| --- | --- | --- |
| `auth` | Login HTTP y restauración/renovación de sesión | OBS-002, OBS-003, OBS-013 |
| `seguridad` | Usuarios, roles, permisos, dispositivos y sesiones | La visibilidad del menú no reemplaza autorización del recurso; falta cierre completo del ciclo de contraseña |
| `organizacion` | Empresas, establecimientos, almacenes, terminales y puesto de trabajo | Funciones profesionales/sanitarias y reglas jerárquicas incompletas |
| `catalogo` | SKU, producto regulado, soporte, marcas, categorías, rubros, códigos | Atributos avanzados no aplicados por todos los flujos |
| `compras` | Proveedores, órdenes, detalle y recepción | Recepción general exige lote; costos sin propagación a inventario |
| `inventario` | Posiciones, detalle de lote, ajustes y bloqueo | No hay interfaz completa de transferencias, conteo, reservas y reposición |
| `pos` | Búsqueda, carrito, cantidad/precio, efectivo y comprobante | Sin receta, cliente, motor fiscal, promociones ni pago electrónico |
| `ventas` | Listado, detalle y anulación | Sin devolución parcial ni ciclo fiscal |
| `caja` | Apertura, turno actual y cierre | No es conciliación multimedio ni tesorería |
| `clientes` | Ruta y pantalla descriptiva | Placeholder |
| `dashboard` | Consulta de resumen y tarjetas | Endpoint no encontrado; ventas, saludo y fecha fijos: OBS-014 |
| `perfil` | Rutas de perfil, seguridad, configuración, sucursal y ayuda | Las páginas inspeccionadas son placeholders; no acreditar autoservicio |

La tabla mantiene los nombres de los 12 directorios de funcionalidades como referencia; el conteo no representa un porcentaje de avance.

## 5. Componentes compartidos y verificación transversal

| Componente | Lo observado | Límite |
| --- | --- | --- |
| `shared-kernel` | Result, errores, entidades, agregado y evento de dominio | Abstracciones disponibles; no prueba que se publiquen eventos de negocio |
| `shared-application` | Puertos de reloj/identificador/eventos, errores y CQRS | Contratos, no procesos terminados |
| `shared-web` | Mapeo de errores y validación HTTP con Problem Details | No reemplaza reglas de negocio ni prueba de todos los fallos de infraestructura |
| `shared-persistence` | Transacciones habilitadas | No se encontró configuración activa de RLS por contexto de tenant |
| `bootstrap-app` | Ensamblado, Flyway, configuración, pruebas de integración centralizadas | No se validaron capacidad productiva, restauración o failover |
| `testing:architecture-tests` | Reglas de dependencias y Spring Modulith | Límites de código no garantizan integridad funcional |
| `testing:test-fixtures` | Infraestructura para pruebas | No es una capacidad comercial |
| `api-client` frontend | Bearer, timeout, traducción de error y reintento tras renovar | Necesita aislamiento y cancelación coordinados con sesión |
| `ui-web` frontend | Componentes reutilizables | Consistencia visual no implica proceso integrado |

La evidencia de idempotencia, bloqueo de filas, control de versión y rollback es positiva y debe preservarse. Se necesita probar esas garantías con varios terminales y datos reales, además de las pruebas unitarias existentes.

## 6. Hallazgos adicionales

Los siguientes hallazgos están confirmados por inspección del código o de sus contratos. Sus escenarios de ejecución siguen pendientes, salvo las comprobaciones estructurales indicadas. Se distingue defecto de integración, capacidad ausente y decisión de política pendiente.

### OBS-007 — Alta: el costo de recepción no llega al inventario

**Tipo:** integración incompleta con efecto en valoración.

**Evidencia:** [RecepcionJdbcWriteAdapter](../../service-botica/modules/compras/src/main/java/com/softprimesolutions/compras/infrastructure/persistence/write/adapter/RecepcionJdbcWriteAdapter.java) persiste `costo_unitario`. [IngresoInventarioPort](../../service-botica/modules/compras/src/main/java/com/softprimesolutions/compras/application/port/out/IngresoInventarioPort.java) no incluye costo ni moneda en `IngresoSolicitado`. [InventarioJdbcWriteAdapter](../../service-botica/modules/inventario/src/main/java/com/softprimesolutions/inventario/infrastructure/persistence/write/adapter/InventarioJdbcWriteAdapter.java) inserta y actualiza cantidades sin alimentar costos. En V005, `costo_promedio`, `costo_unitario` y `costo_total` tienen default cero.

**Escenario:** recibir diez unidades a costo cinco sobre una posición nueva. La recepción puede conservar el costo, mientras posición y movimiento mantienen los defaults, sin valorar la entrada en cincuenta. No se ejecutó este caso en base de datos.

**Acción/cierre:** definir valoración, moneda, descuentos/gastos e impuestos del costo; transportarlos entre módulos y actualizar stock valorado de forma transaccional. Probar entradas a costos distintos, salidas y reintegros; no calcular márgenes con esos ceros como si fueran costos reales.

### OBS-008 — Alta: impuestos de venta fijados en cero

**Tipo:** cálculo monetario incompleto.

**Evidencia:** [VentaJdbcWriteAdapter](../../service-botica/modules/ventas/src/main/java/com/softprimesolutions/ventas/infrastructure/persistence/write/adapter/VentaJdbcWriteAdapter.java) escribe cero en `impuesto_total` e `impuesto` de línea. El SKU modela `afectoIgv`, pero [ReferenciasVentasJdbcAdapter](../../service-botica/modules/ventas/src/main/java/com/softprimesolutions/ventas/infrastructure/persistence/write/adapter/ReferenciasVentasJdbcAdapter.java) no lo recupera. El comprobante interno muestra una línea IGV.

**Escenario:** registrar una venta de un SKU marcado afecto. El flujo no calcula ni desglosa impuesto a partir de esa configuración. Un precio que incluya impuestos no convierte un desglose cero en un desglose correcto.

**Acción/cierre:** definir el tratamiento de precios y afectación, cálculo por línea y redondeo; probar ventas mixtas y conciliación cabecera/líneas. Esta observación no determina una tasa ni tratamiento tributario específico: deben validarse para el negocio.

### OBS-009 — Alta: suspensión del establecimiento no bloquea ventas de sus terminales

**Tipo:** regla organizacional no propagada.

**Evidencia:** [CambiarEstadoEstablecimientoHandler](../../service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/command/CambiarEstadoEstablecimientoHandler.java) persiste el estado del establecimiento. En [ReferenciasVentasJdbcAdapter](../../service-botica/modules/ventas/src/main/java/com/softprimesolutions/ventas/infrastructure/persistence/write/adapter/ReferenciasVentasJdbcAdapter.java), `operable` de terminal depende de la terminal y `operable` de almacén de sus propios flags; no comprueban el estado del establecimiento ni de la empresa.

**Escenario:** mantener turno abierto y terminal/almacén activos; cambiar el establecimiento a suspendido o clausurado; intentar otra venta. Las referencias del flujo revisado siguen calificando los hijos como operables.

**Acción/cierre:** resolver operabilidad de toda la jerarquía al confirmar una operación. Probar el cambio de estado con turnos abiertos y definir expresamente qué cierres, anulaciones o acciones de recuperación se permiten.

### OBS-010 — Alta: productos sin lote/vencimiento no recorren el flujo completo

**Tipo:** contradicción entre catálogo, proceso y DDL.

**Evidencia:** `SKUComercial` admite `requiereLote` y `requiereVencimiento`; [Recepcion.errorItem](../../service-botica/modules/compras/src/main/java/com/softprimesolutions/compras/domain/model/Recepcion.java) exige lote y fecha en todos los ítems. [V005](../cadena-farmacias-docs/database/migrations/V005__inventario_lotes_transferencias.sql) exige `lote_id` en posiciones/movimientos y vencimiento en lote.

**Escenario:** crear un SKU no regulado con ambos flags desactivados y recibirlo sin lote/fecha. El catálogo permite expresar una política que la recepción y la estructura de inventario no soportan.

**Acción/cierre:** decidir trazabilidad por tipo de SKU y aplicarla coherentemente. No usar lotes o vencimientos ficticios como solución general. Probar productos con ambos requisitos, solo lote y sin trazabilidad por lote, según el alcance aprobado.

### OBS-011 — Alta: fraccionamiento sin conversión ni granularidad del SKU

**Tipo:** semántica de unidades incompleta.

**Evidencia:** [LineaVenta.nueva](../../service-botica/modules/ventas/src/main/java/com/softprimesolutions/ventas/domain/model/LineaVenta.java) determina fracción por la presencia de decimales y valida solo el booleano `permiteFraccion`. No recibe `factorFraccion` ni `unidadFraccionCodigo`. El [carrito](../../frontend/apps/erp-web/src/features/pos/lib/carrito.ts) conserva una unidad de venta y una cantidad; el descuento de stock usa esa cantidad.

**Escenario:** SKU de caja con diez unidades, habilitado para fraccionar. Una cantidad `0.15` satisface la regla decimal aunque represente una fracción incompatible con unidades indivisibles; no existe selección explícita de caja versus unidad ni conversión documentada en el flujo.

**Acción/cierre:** definir unidad base, presentaciones y factor; validar múltiplos, convertir cantidades/precios y preservar la unidad vendida. Probar caja/blíster/unidad, venta de una unidad, remanentes y anulación sin deriva de stock.

### OBS-012 — Media: vencimiento usa días UTC y consultas usan la fecha de la base

**Tipo:** inconsistencia temporal.

**Evidencia:** [RegistrarSalidaVentaHandler](../../service-botica/modules/inventario/src/main/java/com/softprimesolutions/inventario/application/usecase/command/RegistrarSalidaVentaHandler.java), `RegistrarMovimientoHandler` y `CambioEstadoLote` calculan el día con UTC. [InventarioJdbcReadAdapter](../../service-botica/modules/inventario/src/main/java/com/softprimesolutions/inventario/infrastructure/persistence/read/adapter/InventarioJdbcReadAdapter.java) usa `CURRENT_DATE`. Compras usa explícitamente `America/Lima`; organización almacena zona horaria.

**Escenario:** a las 20:00 del 10 de octubre en Lima ya es 11 de octubre UTC. Un lote con vencimiento el día 10 queda excluido por escritura cinco horas antes del cambio de fecha local. Lectura y escritura pueden discrepar si la sesión de PostgreSQL usa otra zona.

**Acción/cierre:** acordar la zona y política del día de vencimiento, centralizar el cálculo y usarla en lectura/escritura. Probar 18:59, 19:00, 23:59 y 00:00 locales con reloj controlado. Guardar instantes en UTC sigue siendo compatible con usar días de negocio locales.

### OBS-013 — Media: recuperación de contraseña sin entrega de notificación

**Tipo:** proceso incompleto.

**Evidencia:** [SpringPasswordResetEventPublisher](../../service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/client/identity/SpringPasswordResetEventPublisher.java) publica `PasswordResetRequested`. La búsqueda de consumidores y adaptadores de correo en producción no encontró una entrega; `notificaciones` sigue sin implementación. La prueba de integración captura el evento internamente.

**Impacto:** generar token y responder correctamente al endpoint no garantiza que el usuario pueda recuperar la cuenta. El autoservicio de frontend tampoco está implementado.

**Acción/cierre:** consumidor con proveedor de entrega, caducidad, reintentos y protección del token; interfaz de solicitud/restablecimiento. Validar en un buzón de prueba sin leer directamente la base ni capturar eventos de test.

### OBS-014 — Media: dashboard con datos fijos y resumen sin endpoint implementado

**Tipo:** defecto de integración y representación.

**Evidencia:** [DashboardPage](../../frontend/apps/erp-web/src/features/dashboard/pages/DashboardPage.tsx) contiene ventas recientes, fecha, saludo y alerta de reposición fijos. [dashboard.api.ts](../../frontend/apps/erp-web/src/features/dashboard/api/dashboard.api.ts) llama `/dashboard/summary`; no se encontró controlador correspondiente en `service-botica`. Los [mocks E2E](../../frontend/e2e/support/login.ts) suministran el resumen.

**Impacto:** los datos de ejemplo pueden interpretarse como operación real; en modo HTTP propio del proyecto el resumen no tiene implementación identificada.

**Acción/cierre:** implementar consulta autorizada con fechas y ámbito, o identificar claramente la demostración y retirar indicadores operativos ficticios. Probar que una venta, anulación y movimiento cambien los valores esperados.

### OBS-015 — Alta: el estado sanitario del producto no gobierna su venta

**Tipo:** regla de admisibilidad ausente; amplía OBS-004.

**Evidencia:** [EstadoRegulatorio](../../service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoRegulatorio.java) incluye vigente, vencido, suspendido, cancelado y por validar. La consulta de referencias de venta valida únicamente actividad comercial del SKU y no consulta el producto regulado.

**Escenario:** SKU comercial activo con producto regulado suspendido/cancelado. La validación revisada no rechaza esa relación por su estado sanitario, aunque todavía pueda fallar por stock u otra causa independiente.

**Acción/cierre:** definir y aplicar la política de admisibilidad del producto, del establecimiento y de la operación. Probar el cambio sanitario sin depender de desactivar manualmente todos los SKU vinculados.

### OBS-016 — Media: precio enviado por el cliente sin política de autorización específica

**Tipo:** brecha de gobierno comercial; decisión de negocio pendiente.

**Evidencia:** el carrito permite editar precio; [RegistrarVentaHandler](../../service-botica/modules/ventas/src/main/java/com/softprimesolutions/ventas/application/usecase/command/RegistrarVentaHandler.java) usa `entrada.precioUnitario()` y valida formato/rango, sin compararlo con precio de referencia o lista. El permiso requerido es registrar ventas.

**Impacto:** cualquier usuario autorizado a vender puede proponer precios distintos desde interfaz o petición directa. Esto puede ser una decisión del MVP, pero no equivale a control de descuentos, margen o promociones de una cadena.

**Acción/cierre:** decidir si el precio es libre; en caso contrario calcularlo en servidor y exigir permisos, límites y motivo para excepciones. Probar intento de cambio por cajero, aprobación por supervisor y trazabilidad. No se clasifica toda edición manual como vulnerabilidad sin esa política.

### OBS-017 — Alta: auditoría de negocio diseñada, sin escritura operativa identificada

**Tipo:** capacidad transversal ausente.

**Evidencia:** [V014](../cadena-farmacias-docs/database/migrations/V014__auditoria.sql) crea eventos de auditoría, acceso sensible y exportaciones. No se encontraron escrituras a `sch_auditoria` en el código Java de producción revisado. Los campos `created_by`/`updated_by` y movimientos de inventario cubren parte de la trazabilidad, pero no sustituyen un historial de cambios de permisos, decisiones y accesos.

**Acción/cierre:** definir eventos críticos, contenido mínimo, protección y consulta por ámbito; instrumentar altas/cambios de privilegios, bloqueos, precios excepcionales y acciones sensibles. Probar quién, cuándo, recurso, resultado y correlación, evitando almacenar secretos.

### OBS-018 — Media: E2E de interfaz no acredita integración con backend real

**Tipo:** brecha de evidencia de pruebas.

**Evidencia:** [login.ts](../../frontend/e2e/support/login.ts) intercepta login, refresh y dashboard; [pos-api.ts](../../frontend/e2e/support/pos-api.ts) intercepta catálogo, inventario y venta con `page.route`/`route.fulfill`. Esas pruebas son útiles para UI, pero no ejercitan Spring, transacciones ni PostgreSQL.

**Acción/cierre:** conservar los tests de interfaz y agregar un grupo identificado que use backend y base reales. Ejecutar compra → recepción → venta → cierre, con comprobaciones SQL y pruebas de ámbitos/concurrencia. No presentar resultados simulados como aceptación integral.

### OBS-019 — Media: benchmarking documental confunde objetivo con capacidad ejecutable

**Tipo:** calidad de información para decisiones.

**Evidencia:** [benchmarking existente](../cadena-farmacias-docs/docs/01-negocio/02-benchmarking.md) marca «Sí» en Proyecto para transferencias, reposición, dispensación, promociones y fidelización; no se encontraron sus procesos implementados. Las tablas y módulos previstos no justifican esas marcas como estado actual.

**Acción/cierre:** separar objetivo de producto, diseño, implementación e integración validada; enlazar cada afirmación a evidencia. La nueva [comparativa](2026-10-10-comparativa-mercado.md) sirve como diagnóstico actualizado sin sobrescribir automáticamente decisiones del documento canónico.

## 7. Matriz de validación pendiente

Todos los casos siguientes están pendientes de ejecución específica. Son criterios de aceptación propuestos, no resultados de pruebas.

| Caso | Flujo o condición | Resultado esperado |
| --- | --- | --- |
| V01 | Usuario de sucursal A consulta/crea en B | Rechazo por ámbito antes de exponer o modificar datos |
| V02 | Dos tenants con identificadores cruzados | Rechazo en API y restricciones de datos |
| V03 | Logout/login de otra cuenta con consultas pendientes | Ningún dato anterior aparece ni repuebla caché |
| V04 | Credencial con cambio obligatorio | Completa cambio antes de ingresar al negocio |
| V05 | Recuperación desde pantalla hasta buzón de prueba | Entrega y uso único del token, con caducidad |
| V06 | Dos proveedores/órdenes, recepción parcial y exceso | Reglas de cantidad y estado correctas |
| V07 | Repetir recepción con misma clave y distinto cuerpo | Sin duplicación; conflicto cuando corresponda |
| V08 | Recepciones a costos diferentes | Valoración conciliada con método aprobado |
| V09 | Error en ingreso de una línea de recepción | Rollback de toda la operación requerida |
| V10 | SKU sin lote o sin vencimiento | Flujo coherente con política de trazabilidad |
| V11 | Caja, blíster y unidad del mismo producto | Conversión exacta y stock coherente |
| V12 | Dos terminales compiten por último stock | Sin sobreventa ni cantidades negativas |
| V13 | Múltiples lotes y cantidades reservadas | FEFO y disponibilidad correctos |
| V14 | Venta frente a bloqueo concurrente de lote | Resultado seguro según regla de concurrencia aprobada |
| V15 | Cambio de día local/UTC y vencimiento hoy | Lectura y escritura coinciden |
| V16 | Producto suspendido/cancelado o con receta requerida | Rechazo o validación profesional exigida |
| V17 | Establecimiento suspendido con turno abierto | Nuevas ventas bloqueadas; recuperación controlada |
| V18 | SKU afecto/no afecto y redondeos | Totales y desglose fiscal consistentes |
| V19 | Cambio de precio por rol no autorizado | Rechazo o aprobación explícita según política |
| V20 | Repetición de venta después de timeout | Una sola venta y un solo descuento de stock |
| V21 | Anulación y cierre simultáneos | Stock, efectivo y estado de turno conciliados |
| V22 | Devolución parcial después de cierre | Flujo separado con devolución monetaria y tratamiento fiscal |
| V23 | CPE aceptado/rechazado/reintento | Estados y documentos trazables sin duplicación |
| V24 | Pago externo pendiente/fallido/reversado | No se confirma una venta cobrada inexistente |
| V25 | Transferencia con faltante o daño | Despacho, tránsito, recepción y diferencia conciliados |
| V26 | Corte de red durante venta | Comportamiento definido; si se promete offline, sincronización sin duplicados |
| V27 | Pedido web concurrente con venta de mostrador | Reserva, vencimiento y disponibilidad consistentes |
| V28 | Canje/reversa/caducidad de beneficio | Saldos y movimientos auditables |
| V29 | Cambio de rol o bloqueo y lectura de auditoría | Evento completo, autorizado y sin secretos |
| V30 | Dashboard tras operaciones reales | Datos actuales, ámbito correcto y estado de error inequívoco |
| V31 | Migración desde instalación vacía y desde versión previa | Esquema válido y datos preservados |
| V32 | Restauración de backup y carga representativa | Objetivos de recuperación y latencia medidos |

## 8. Prioridades propuestas

1. Cerrar OBS-001–005 y OBS-007–015 antes de aceptar operación con datos y surtido reales; priorizar aislamiento, admisibilidad, dinero y consistencia de stock.
2. Definir el mínimo operativo de una botica: facturación o integración fiscal, medios de pago requeridos, clientes, devoluciones, auditoría y pruebas integrales.
3. Completar operación de cadena: transferencias, conteos, reposición, costos, precios centralizados y permisos por local.
4. Construir diferenciación comercial: fidelización, promociones y omnicanalidad sobre ese núcleo conciliado.
5. Decidir qué capacidades se construyen y cuáles se integran: finanzas, RR. HH., proveedor fiscal, pagos y mensajería. No se asignan fechas sin estimar equipo y dependencias.

No hay evidencia suficiente para declarar equivalencia con una cadena nacional ni preparación productiva. Sí existe una base modular aprovechable; el siguiente avance debe medirse por procesos completos validados, no por número de tablas o pantallas.
