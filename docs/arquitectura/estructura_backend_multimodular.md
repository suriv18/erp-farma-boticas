# Estructura multimodular rica del backend

> **Estado: LEGADO / REFERENCIA DE ESTRUCTURA.** Para decisiones vigentes y estado real consulte
> [`GOV-FAR-001`](../cadena-farmacias-docs/docs/00-gobierno/01-fuente-unica-verdad.md) y los ADR
> canónicos. Los ejemplos de este documento no implican funcionalidad implementada.

**Patrones combinados:** DDD + Clean Architecture + Ports and Adapters + CQRS + Result Pattern  
**Estilo de solución:** monolito modular evolutivo  
**Base tecnológica:** Java 25 + Spring Boot 4.x + Gradle 9.x + PostgreSQL + Spring Modulith  
**Última revisión técnica:** 2026-08-09

## 1. Objetivo

Esta estructura organiza el backend del ERP Boticas como un **monolito modular con Gradle**, Java 25 y Spring Boot 4.x.

El proyecto tendrá un proyecto raíz especial llamado `backend-parent`. Este padre no contiene reglas de negocio ni genera una aplicación ejecutable. Su responsabilidad es centralizar:

- Versiones de Java, plugins y dependencias.
- Repositorios Maven.
- Reglas de compilación.
- Configuración común de pruebas.
- Calidad de código.
- Convenciones de nombres y empaquetado.

El código Java reutilizable se ubica en módulos `shared-*`. De este modo se evita copiar configuraciones o utilidades, sin convertir el módulo padre en un contenedor de lógica de negocio.

## 2. Principios

1. `backend-parent` configura y ensambla; no contiene código productivo.
2. `bootstrap-app` es el único módulo ejecutable de Spring Boot.
3. Cada módulo de negocio representa un bounded context.
4. Un módulo no puede importar entidades JPA ni repositorios internos de otro módulo.
5. La comunicación entre módulos se realiza mediante puertos públicos, servicios de aplicación o eventos.
6. `shared-kernel` solo contiene conceptos universales y estables.
7. No se comparte lógica por comodidad; se comparte únicamente cuando existe una abstracción común real.
8. Las dependencias siempre apuntan hacia el dominio.

## 3. Árbol del proyecto

```text
backend/
├── settings.gradle
├── build.gradle                     # Proyecto padre: backend-parent
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/
│   ├── libs.versions.toml           # Catálogo central de versiones
│   └── wrapper/
├── build-logic/                     # Plugins de convención Gradle
│   ├── settings.gradle
│   └── src/main/groovy/
│       ├── boticas.java-library.gradle
│       ├── boticas.spring-module.gradle
│       ├── boticas.test.gradle
│       └── boticas.quality.gradle
├── bootstrap-app/                   # Único ejecutable Spring Boot
├── shared-kernel/                   # Tipos compartidos sin Spring
├── shared-application/              # Contratos transversales de casos de uso
├── shared-web/                      # Respuestas HTTP, errores y filtros comunes
├── shared-persistence/              # Configuración técnica común de persistencia
├── modules/
│   ├── security/
│   ├── organizacion/
│   ├── catalogo/
│   ├── farmacia/
│   ├── inventario/
│   ├── compras/
│   ├── ventas/
│   ├── clientes/
│   ├── finanzas/
│   ├── rrhh/
│   ├── crm/
│   ├── cmr/
│   ├── app-channel/
│   ├── logistica/
│   ├── pagos/
│   ├── notificaciones/
│   └── integraciones/
└── testing/
    ├── test-fixtures/                # Builders y datos reutilizables de prueba
    └── architecture-tests/           # Reglas ArchUnit entre módulos y capas
```

## 4. Responsabilidad del padre `backend-parent`

En Gradle el proyecto raíz cumple el papel de padre y agregador. No se debe crear una clase Java `BaseModule` para que todos los módulos hereden de ella. La reutilización se consigue mediante configuración Gradle, composición y dependencias explícitas.

### `settings.gradle`

```groovy
pluginManagement {
    includeBuild('build-logic')
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = 'backend-parent'

include 'bootstrap-app'
include 'shared-kernel'
include 'shared-application'
include 'shared-web'
include 'shared-persistence'

include 'modules:security'
include 'modules:organizacion'
include 'modules:catalogo'
include 'modules:farmacia'
include 'modules:inventario'
include 'modules:compras'
include 'modules:ventas'
include 'modules:clientes'
include 'modules:finanzas'
include 'modules:rrhh'
include 'modules:crm'
include 'modules:cmr'
include 'modules:app-channel'
include 'modules:logistica'
include 'modules:pagos'
include 'modules:notificaciones'
include 'modules:integraciones'

include 'testing:test-fixtures'
include 'testing:architecture-tests'
```

Gradle carga automáticamente el catálogo convencional `gradle/libs.versions.toml`. No debe declararse nuevamente en `versionCatalogs`, porque se duplicaría el catálogo `libs`.

### `build.gradle` del padre

```groovy
plugins {
    id 'base'
    id 'org.springframework.boot' version '4.1.0' apply false
    id 'io.spring.dependency-management' version '1.1.7' apply false
}

group = 'pe.com.boticas'
version = '1.0.0-SNAPSHOT'

allprojects {
    group = rootProject.group
    version = rootProject.version
}

tasks.register('testAll') {
    dependsOn subprojects.collect { project -> project.tasks.matching { it.name == 'test' } }
}
```

El padre centraliza las versiones y tareas agregadas. Los plugins de `build-logic` centralizan la configuración repetida de los subproyectos.

## 5. Módulos compartidos

### `shared-kernel`

No depende de Spring, JPA ni infraestructura.

```text
shared-kernel/src/main/java/pe/com/boticas/shared/
├── result/
│   ├── Result.java
│   └── ErrorDetail.java
├── domain/
│   ├── AggregateRoot.java
│   ├── DomainEvent.java
│   └── EntityId.java
├── valueobject/
│   ├── Money.java
│   ├── Email.java
│   ├── DocumentoIdentidad.java
│   └── Telefono.java
└── exception/
    └── TechnicalException.java
```

No debe contener `Producto`, `Venta`, `Cliente`, `Stock`, DTO REST ni entidades JPA.

### `shared-application`

Contiene contratos técnicos utilizados por varios casos de uso:

- `CommandHandler` y `QueryHandler`.
- Paginación independiente de Spring.
- Contexto del usuario autenticado.
- Reloj y generador de identificadores como puertos.
- Publicador de eventos.
- Idempotencia.

### `shared-web`

Centraliza elementos HTTP para no repetirlos en cada módulo:

- `ApiResponse<T>`.
- `GlobalExceptionHandler`.
- Mapeo de errores de aplicación a códigos HTTP.
- Filtros de correlación y logging.
- Configuración común de OpenAPI.
- Validaciones y serialización comunes.

`shared-web` puede depender de `shared-application` y `shared-kernel`, pero nunca de módulos de negocio.

### `shared-persistence`

Centraliza exclusivamente infraestructura repetible:

- Configuración JPA.
- Conversores de tipos comunes.
- Auditoría técnica.
- Configuración de transacciones.
- Soporte de outbox e idempotencia.

No debe incluir repositorios concretos de ventas, inventario o cualquier otro dominio.

## 6. Estructura interna de un módulo de negocio

Cada bounded context es un subproyecto Gradle independiente. Ejemplo para `ventas`:

```text
modules/ventas/
├── build.gradle
└── src/
    ├── main/java/pe/com/boticas/ventas/
    │   ├── domain/
    │   │   ├── model/
    │   │   ├── valueobject/
    │   │   ├── service/
    │   │   ├── event/
    │   │   └── repository/
    │   ├── application/
    │   │   ├── port/in/             # Puertos específicos de casos de uso
    │   │   ├── port/out/
    │   │   ├── command/
    │   │   ├── query/
    │   │   ├── handler/
    │   │   └── dto/
    │   ├── adapter/in/
    │   │   ├── rest/
    │   │   ├── messaging/
    │   │   └── scheduler/
    │   ├── adapter/out/
    │   │   ├── persistence/
    │   │   ├── integration/
    │   │   ├── events/
    │   │   └── configuration/
    │   └── api/                     # Contratos públicos para otros módulos
    └── test/java/pe/com/boticas/ventas/
        ├── domain/
        ├── application/
        ├── adapter/in/
        └── adapter/out/
```

La carpeta `api` es la única superficie que otros módulos pueden consumir directamente. No expone entidades JPA. ArchUnit debe impedir imports hacia cualquier otro paquete del módulo.

Cuando sea necesario aislamiento de compilación más estricto, un contexto crítico puede dividirse en dos subproyectos:

```text
modules/inventario/
├── inventario-api/          # Contratos estables, comandos, consultas y eventos públicos
└── inventario-impl/         # Dominio, casos de uso, persistencia y controladores
```

Los consumidores dependen de `inventario-api`; `bootstrap-app` ensambla también `inventario-impl`. Esta separación debe aplicarse donde exista consumo real entre contextos, no automáticamente a todos los módulos.

## 7. Dependencias permitidas

```text
bootstrap-app
    └── todos los módulos de negocio y shared-*

módulo de negocio
    ├── shared-kernel
    ├── shared-application
    ├── shared-web             solo si publica endpoints
    └── shared-persistence     solo desde adapter/out

domain
    └── shared-kernel

application
    ├── domain
    ├── shared-kernel
    └── shared-application

adapter/out
    ├── application
    └── domain

adapter/in
    └── application
```

No están permitidas dependencias circulares como `ventas -> inventario -> ventas`.

Cuando ventas necesite stock, define `StockPort` en su capa Application. Un adaptador o contrato público de Inventario implementa ese puerto durante el ensamblaje.

## 8. `build.gradle` de un módulo

```groovy
plugins {
    id 'boticas.spring-module'
    id 'boticas.test'
    id 'boticas.quality'
}

dependencies {
    implementation project(':shared-kernel')
    implementation project(':shared-application')
    implementation project(':shared-web')
    implementation project(':shared-persistence')

    testImplementation(testFixtures(project(':testing:test-fixtures')))
}
```

Las versiones de librerías y la configuración de Java no se repiten porque son proporcionadas por los plugins de convención y `libs.versions.toml`.

## 9. Módulo ejecutable `bootstrap-app`

`bootstrap-app` contiene únicamente el punto de entrada, configuración global y ensamblaje.

```text
bootstrap-app/src/main/
├── java/pe/com/boticas/
│   ├── ErpBoticasApplication.java
│   └── bootstrap/configuration/
└── resources/
    ├── application.yml
    ├── application-dev.yml
    ├── application-test.yml
    ├── application-prod.yml
    └── db/migration/
```

No debe contener controladores, reglas de negocio ni repositorios propios.

## 10. Pruebas y control arquitectónico

```text
Domain         -> pruebas unitarias puras
Application    -> pruebas unitarias con puertos simulados
Infrastructure -> Testcontainers con PostgreSQL
Interfaces     -> MockMvc o pruebas HTTP
Arquitectura   -> ArchUnit
Flujos críticos-> pruebas de integración desde bootstrap-app
```

Las pruebas ArchUnit deben comprobar, como mínimo:

- Domain no depende de Spring, JPA ni Interfaces.
- Application no depende de Infrastructure.
- Las entidades JPA no salen de Infrastructure.
- Un módulo no accede a paquetes internos de otro módulo.
- Solo `bootstrap-app` utiliza `@SpringBootApplication`.

## 11. Regla para evitar un módulo compartido gigante

Antes de mover código a `shared-*`, deben cumplirse estas condiciones:

1. Lo utilizan al menos dos módulos.
2. Tiene el mismo significado en ambos contextos.
3. No contiene una regla propia de un negocio particular.
4. Su cambio no obliga a coordinar dominios que deberían ser independientes.

Si una clase no cumple esas condiciones, debe permanecer en su módulo aunque exista código parecido.

## 12. Resultado esperado

La estructura separa tres tipos de reutilización:

- `backend-parent` y `build-logic`: configuración de construcción reutilizable.
- `shared-*`: código técnico o conceptual realmente común.
- `modules/*/api`: contratos explícitos entre dominios.

Así se reduce la duplicación sin crear herencia artificial, dependencias circulares ni un módulo común que conozca todo el ERP.

## 13. Cómo se combinan los patrones

Los cinco patrones no representan cinco arquitecturas superpuestas. Cada uno resuelve una dimensión diferente:

| Patrón | Decisión que aporta | Aplicación en el ERP |
|---|---|---|
| DDD | Divide el negocio y modela sus reglas | Bounded contexts, agregados, entidades, value objects y eventos de dominio |
| Clean Architecture | Dirige las dependencias hacia las políticas | Domain y Application no conocen HTTP, JPA, PostgreSQL ni Spring MVC |
| Ports and Adapters | Define cómo se cruza el límite de la aplicación | Puertos de entrada para casos de uso y puertos de salida para capacidades externas |
| CQRS | Separa las responsabilidades de escritura y lectura | Commands con dominio transaccional; Queries con DTO y modelos de lectura |
| Result Pattern | Representa resultados esperados sin `null` | Éxito o error de negocio tipado; traducción a RFC 9457 en el adaptador HTTP |

La regla de decisión central es:

```text
El dominio expresa políticas.
Application orquesta intenciones.
Los puertos expresan capacidades.
Los adaptadores resuelven tecnología.
CQRS separa escritura de lectura.
Result expresa desenlaces esperados.
Las excepciones quedan para fallos técnicos o defectos de programación.
```

## 14. Modelo arquitectónico completo

```text
                          ADAPTADORES DE ENTRADA
              REST | eventos | jobs | CLI | pruebas | batch
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│ Interfaces / Inbound Adapters                                   │
│ Controller, Consumer, Scheduler, Request Mapper, Problem Mapper  │
├─────────────────────────────────────────────────────────────────┤
│ Application                                                     │
│ Command Handlers | Query Handlers | Ports In/Out | DTO internos │
│ Policies de autorización | Idempotencia | Orquestación          │
├─────────────────────────────────────────────────────────────────┤
│ Domain                                                          │
│ Aggregates | Entities | Value Objects | Domain Services         │
│ Domain Events | Specifications | Repository contracts           │
├─────────────────────────────────────────────────────────────────┤
│ Infrastructure / Outbound Adapters                              │
│ JPA | JDBC | PostgreSQL | SUNAT | DIGEMID | pagos | mensajería   │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
                         SISTEMAS EXTERNOS
```

Las dependencias de código apuntan hacia adentro. El flujo de ejecución puede ir en ambas direcciones porque los adaptadores implementan interfaces definidas en el núcleo.

## 15. DDD estratégico: límites del negocio

### 15.1. Bounded contexts propuestos

| Contexto | Responsabilidad principal | Dueño de datos | Relación relevante |
|---|---|---|---|
| `security` | Identidad interna, credenciales, sesiones y RBAC | Usuarios, roles, permisos | Provee identidad autenticada |
| `organizacion` | Empresa, sucursales, almacenes y cajas | Estructura organizacional | Referencia estable para operación |
| `rrhh` | Trabajadores, contratos, asignaciones y turnos | Trabajadores | Vincula trabajador con usuario interno |
| `catalogo` | Producto comercial, SKU, categoría, marca y precio base | Catálogo comercial | Publica identidad y datos comerciales del SKU |
| `farmacia` | Reglas sanitarias, receta y datos farmacéuticos | Extensión farmacéutica | Extiende conceptos del catálogo mediante contrato |
| `inventario` | Lotes, stock, reservas, kardex y transferencias | Existencias | Publica disponibilidad y movimientos |
| `compras` | Proveedor, orden, recepción y costo de compra | Ciclo de compra | Solicita ingreso a inventario |
| `ventas` | Venta POS, devoluciones y comprobantes | Ciclo de venta | Consume stock, caja, cliente y facturación |
| `clientes` | Identidad comercial del cliente y direcciones | Cliente maestro | Evita que `ventas` sea dueño del cliente global |
| `crm` | Segmentación, campañas, fidelización y reclamos | Relación comercial | Consume una referencia de cliente |
| `cmr` | Consentimiento, preferencias y autogestión | Relación gestionada por cliente | Consume una referencia de cliente |
| `finanzas` | CxC, CxP, bancos, caja financiera y contabilidad | Movimientos financieros | Reacciona a ventas y compras confirmadas |
| `app-channel` | Cuenta digital, dispositivo, carrito y canal | Experiencia digital | Orquesta casos digitales, no domina ventas |
| `pagos` | Intentos, autorizaciones, webhooks y extornos | Transacción de pago | Publica confirmaciones idempotentes |
| `logistica` | Preparación, despacho, reparto y tracking | Entrega | Reacciona a pedidos confirmados |
| `notificaciones` | Plantillas y entrega de mensajes | Historial de notificación | Consume eventos públicos |
| `integraciones` | Adaptadores SUNAT, DIGEMID y terceros | Bitácora técnica | No concentra reglas de negocio externas |

`clientes` se define como contexto propio porque su identidad es usada por ventas, CRM, CMR, ecommerce y finanzas. Mantenerlo dentro de `ventas` produciría propiedad ambigua y dependencias contra el contexto equivocado.

### 15.2. Context map inicial

```text
security ───────► rrhh                    identidad vinculada
organizacion ──► todos                    referencia organizacional
catalogo ──────► farmacia                 SKU y producto base
catalogo ──────► inventario               SKU inventariable
clientes ──────► ventas / crm / cmr        referencia de cliente
compras ───────► inventario               CompraRecibida
ventas ────────► inventario               reservar/descontar/reponer stock
ventas ────────► pagos                    iniciar/capturar/extornar pago
ventas ────────► integraciones            emitir comprobante
ventas ────────► finanzas                 VentaConfirmada
app-channel ───► ventas                   crear pedido digital
ventas ────────► logistica                PedidoListoParaDespacho
todos ─────────► notificaciones           eventos notificables
```

La flecha indica dependencia del consumidor hacia el contrato publicado por el proveedor; no autoriza acceso directo a sus tablas.

### 15.3. Tipos de integración entre contextos

- **Consulta inmediata:** API pública o puerto cuando el consumidor necesita respuesta sincrónica.
- **Comando explícito:** puerto de entrada del contexto propietario cuando debe ejecutar una capacidad.
- **Evento de módulo:** notificación interna posterior a un hecho confirmado.
- **Evento de integración:** contrato versionado destinado a sistemas externos o futuros microservicios.
- **Anti-Corruption Layer:** obligatorio al integrar SUNAT, DIGEMID, pasarelas o modelos heredados.

## 16. DDD táctico dentro de cada módulo

### 16.1. Agregados

Un agregado es el límite de consistencia transaccional. Solo su raíz puede ser cargada y persistida mediante repositorio.

Agregados candidatos:

```text
Venta
├── DetalleVenta
├── PagoRegistrado
└── PromocionAplicada

ReservaStock
├── ItemReserva
└── FechaExpiracion

OrdenCompra
└── DetalleOrdenCompra

TransferenciaStock
└── DetalleTransferencia

TurnoCaja
└── MovimientoTurno
```

Reglas:

1. Una transacción modifica idealmente un agregado.
2. Otros agregados se referencian por identificador, no por asociación JPA navegable.
3. El constructor no permite estados inválidos.
4. Los cambios ocurren mediante métodos con lenguaje del negocio: `venta.anular(...)`, no `venta.setEstado(...)`.
5. Los eventos se registran después de una transición válida.
6. Los repositorios se definen por raíz de agregado, no por tabla.

### 16.2. Entidades y value objects

```text
Entidad      -> identidad estable y ciclo de vida
Value Object -> igualdad por valor, inmutable y validado al crearse
```

Value objects recomendados:

```text
VentaId, ProductoSkuId, LoteId, ClienteId
Money, Quantity, Percentage
NumeroDocumento, Ruc, Dni
NumeroComprobante, SerieComprobante
Email, Telefono, Ubigeo
DateRange, AuditActor
```

No se deben utilizar `String`, `Long` o `BigDecimal` desnudos cuando el tipo posee invariantes relevantes.

### 16.3. Servicios de dominio

Se utilizan únicamente para reglas que:

- Pertenecen al dominio.
- Involucran varios conceptos.
- No encajan naturalmente en una entidad o value object.

No reciben `EntityManager`, clientes HTTP, DTO REST ni objetos de Spring.

## 17. Estructura rica de un bounded context

```text
modules/ventas/src/main/java/pe/com/boticas/ventas/
├── package-info.java                         # @ApplicationModule
├── api/                                      # API pública del contexto
│   ├── package-info.java                     # @NamedInterface("api")
│   ├── VentaFacade.java
│   ├── VentaSummary.java
│   └── event/
│       ├── VentaConfirmada.java
│       └── VentaAnulada.java
├── domain/
│   ├── model/
│   │   ├── Venta.java                        # Aggregate root
│   │   ├── DetalleVenta.java                 # Entity interna
│   │   └── VentaEstado.java
│   ├── valueobject/
│   │   ├── VentaId.java
│   │   ├── NumeroOperacion.java
│   │   └── Money.java
│   ├── service/
│   │   └── PoliticaAnulacionVenta.java
│   ├── event/
│   │   └── VentaRegistradaDomainEvent.java
│   └── repository/
│       └── VentaRepository.java
├── application/
│   ├── command/
│   │   ├── RegistrarVentaCommand.java
│   │   ├── RegistrarVentaHandler.java
│   │   ├── AnularVentaCommand.java
│   │   └── AnularVentaHandler.java
│   ├── query/
│   │   ├── BuscarVentasQuery.java
│   │   ├── BuscarVentasHandler.java
│   │   └── VentaDetalleView.java
│   ├── port/in/
│   │   ├── RegistrarVentaUseCase.java
│   │   └── BuscarVentasUseCase.java
│   ├── port/out/
│   │   ├── LoadVentaPort.java
│   │   ├── SaveVentaPort.java
│   │   ├── StockPort.java
│   │   ├── CajaPort.java
│   │   ├── VentaReadPort.java
│   │   └── DomainEventPublisher.java
│   ├── policy/
│   │   ├── AuthorizationPolicy.java
│   │   └── IdempotencyPolicy.java
│   └── mapper/
│       └── VentaApplicationMapper.java
├── adapter/in/
│   ├── rest/
│   │   ├── VentaCommandController.java
│   │   ├── VentaQueryController.java
│   │   ├── request/
│   │   └── mapper/
│   ├── messaging/
│   └── scheduler/
├── adapter/out/
│   ├── persistence/write/
│   │   ├── VentaJpaEntity.java
│   │   ├── SpringDataVentaRepository.java
│   │   ├── VentaPersistenceMapper.java
│   │   └── VentaPersistenceAdapter.java
│   ├── persistence/read/
│   │   └── JdbcVentaReadAdapter.java
│   ├── inventory/
│   │   └── InventarioStockAdapter.java
│   ├── events/
│   │   └── SpringDomainEventAdapter.java
│   └── configuration/
│       └── VentasModuleConfiguration.java
└── architecture/
    └── package-info.java
```

La estructura usa `adapter/in` y `adapter/out` para hacer visible la arquitectura hexagonal. `domain` y `application` constituyen el interior del hexágono.

## 18. Reglas de Ports and Adapters

### 18.1. Puertos de entrada

Representan intenciones que conducen la aplicación:

```java
public interface Command<R> {}

public interface Query<R> {}

public interface CommandHandler<C, R> {
    Result<R, ApplicationError> handle(C command);
}

public interface QueryHandler<Q, R> {
    Result<R, ApplicationError> handle(Q query);
}
```

Los puertos de entrada no reciben `HttpServletRequest`, `ResponseEntity`, entidades JPA ni payloads de Kafka.

### 18.2. Puertos de salida

Se nombran por la capacidad que necesita la aplicación:

```text
SaveVentaPort             correcto
LoadStockDisponiblePort   correcto
EmitirComprobantePort     correcto

PostgresRepository        incorrecto: filtra tecnología
SunatServiceImpl          incorrecto: expresa implementación
CommonService             incorrecto: no expresa intención
```

### 18.3. Adaptadores de entrada

- REST controllers.
- Consumers de mensajería.
- Schedulers.
- Batch jobs.
- CLI administrativa.
- Fixtures de prueba que conducen casos de uso.

### 18.4. Adaptadores de salida

- Persistencia JPA/JDBC.
- API de SUNAT o DIGEMID.
- Pasarela de pago.
- Almacenamiento de archivos.
- Correo, SMS, WhatsApp y push.
- Event bus y outbox.

## 19. CQRS pragmático

### 19.1. Nivel recomendado para el MVP

```text
Commands ──► modelo de dominio ──► JPA ───────┐
                                              ├── PostgreSQL
Queries  ──► SQL/JDBC projections ────────────┘
```

Se separan los modelos y el código, pero se conserva una sola base de datos. Esto entrega claridad sin introducir consistencia eventual prematuramente.

No se adopta Event Sourcing en el MVP. CQRS y Event Sourcing son decisiones independientes.

### 19.2. Commands

Un command:

- Expresa una intención de negocio: `ConfirmarVenta`, no `ActualizarVenta`.
- Es inmutable.
- Incluye identificador de idempotencia cuando puede repetirse.
- Se autoriza antes de ejecutar.
- Modifica estado mediante el agregado.
- Devuelve información mínima: identificador, versión y estado confirmado.

```java
public record RegistrarVentaCommand(
        UUID commandId,
        SucursalId sucursalId,
        CajaId cajaId,
        UsuarioId vendedorId,
        ClienteId clienteId,
        List<ItemCommand> items,
        List<PagoCommand> pagos
) implements Command<RegistrarVentaOutput> {}
```

### 19.3. Queries

Una query:

- Nunca modifica estado.
- No carga agregados si solo necesita proyectar datos.
- Devuelve DTO optimizado para el consumidor.
- Puede usar `JdbcClient`, SQL explícito, una vista o jOOQ.
- Aplica autorización y alcance por empresa/sucursal.

```java
public record BuscarVentasQuery(
        EmpresaId empresaId,
        SucursalId sucursalId,
        LocalDate desde,
        LocalDate hasta,
        PageRequest page
) implements Query<Page<VentaSummaryView>> {}
```

### 19.4. Evolución del modelo de lectura

```text
Etapa 1 -> DTO proyectado desde las mismas tablas PostgreSQL
Etapa 2 -> vistas/materialized views para reportes costosos
Etapa 3 -> esquema de lectura separado en PostgreSQL
Etapa 4 -> almacén de lectura independiente si la escala lo exige
```

Cada evolución debe estar respaldada por métricas. No se introduce consistencia eventual sin una necesidad observable.

## 20. Result Pattern sin `null`

### 20.1. Contrato base

```java
package pe.com.boticas.shared.result;

import java.util.Objects;
import java.util.function.Function;

public sealed interface Result<T, E>
        permits Result.Success, Result.Failure {

    <R> R fold(
            Function<? super T, ? extends R> onSuccess,
            Function<? super E, ? extends R> onFailure
    );

    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    record Success<T, E>(T value) implements Result<T, E> {
        public Success {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public <R> R fold(
                Function<? super T, ? extends R> onSuccess,
                Function<? super E, ? extends R> onFailure
        ) {
            return onSuccess.apply(value);
        }
    }

    record Failure<T, E>(E error) implements Result<T, E> {
        public Failure {
            Objects.requireNonNull(error, "error");
        }

        @Override
        public <R> R fold(
                Function<? super T, ? extends R> onSuccess,
                Function<? super E, ? extends R> onFailure
        ) {
            return onFailure.apply(error);
        }
    }
}
```

Este diseño no ofrece métodos parciales como `value()` en un fallo ni `error()` en un éxito; por tanto, no necesita retornar `null`.

### 20.2. Jerarquía de errores esperados

```java
public interface ApplicationError {

    String code();
    String message();
    ErrorCategory category();
}
```

Clasificación:

| Tipo | Ejemplo | HTTP habitual |
|---|---|---:|
| `ValidationError` | Cantidad inválida | 400 o 422 |
| `NotFoundError` | Venta inexistente | 404 |
| `ConflictError` | Versión desactualizada | 409 |
| `ForbiddenError` | Usuario sin permiso | 403 |
| `BusinessRuleError` | Caja cerrada o stock insuficiente | 409 o 422 |

El código de dominio no conoce los códigos HTTP. El adaptador web realiza el mapeo.

### 20.3. Result no reemplaza excepciones técnicas

El agregado puede utilizar su propio error de dominio, por ejemplo `Result<Venta, VentaError>`. El handler lo traduce a `ApplicationError`; así Domain no depende de Application.

Usar `Result.failure` para errores esperados. Lanzar una excepción técnica para:

- Base de datos no disponible.
- Timeout de infraestructura.
- Error de serialización.
- Violación inesperada de una invariante.
- Defecto de programación.

La excepción técnica se registra con correlation ID y se traduce a un problema HTTP genérico, sin exponer detalles internos.

## 21. Result y transacciones

Retornar un `Failure` no hace rollback automáticamente en Spring. Un command bus transaccional debe marcar rollback cuando el resultado falle:

```java
public final class TransactionalCommandExecutor {

    private final TransactionTemplate transactions;

    public TransactionalCommandExecutor(TransactionTemplate transactions) {
        this.transactions = transactions;
    }

    public <C, R> Result<R, ApplicationError> execute(
            CommandHandler<C, R> handler,
            C command
    ) {
        return transactions.execute(status -> {
            Result<R, ApplicationError> result = handler.handle(command);

            if (result instanceof Result.Failure<?, ?>) {
                status.setRollbackOnly();
            }

            return result;
        });
    }
}
```

Reglas transaccionales:

1. Un command abre una transacción en el borde de Application.
2. Las queries usan transacción `readOnly` cuando sea necesario.
3. No se mantiene una transacción abierta durante una llamada remota lenta.
4. Los errores técnicos provocan excepción y rollback.
5. Un `Failure` posterior a una escritura también provoca rollback.
6. El evento durable se registra dentro de la misma transacción que el agregado.

## 22. Ejemplo completo: registrar venta

### 22.1. Flujo

```text
HTTP POST /v1/ventas
    │
    ▼
VentaCommandController
    │ request -> command
    ▼
TransactionalCommandExecutor
    │
    ▼
RegistrarVentaHandler
    ├── AuthorizationPolicy
    ├── IdempotencyPolicy
    ├── CajaPort
    ├── StockPort
    ├── Venta.crear(...)
    ├── SaveVentaPort
    └── DomainEventPublisher
    │
    ▼
Result<RegistrarVentaOutput, ApplicationError>
    │
    ├── Success -> HTTP 201
    └── Failure -> RFC 9457
```

### 22.2. Handler

```java
public final class RegistrarVentaHandler
        implements CommandHandler<RegistrarVentaCommand, RegistrarVentaOutput> {

    private final CajaPort caja;
    private final StockPort stock;
    private final SaveVentaPort ventas;
    private final DomainEventPublisher events;

    @Override
    public Result<RegistrarVentaOutput, ApplicationError> handle(
            RegistrarVentaCommand command
    ) {
        var cajaResult = caja.validarAbierta(command.cajaId(), command.vendedorId());

        return cajaResult.fold(
                cajaAbierta -> crearVenta(command),
                Result::failure
        );
    }

    private Result<RegistrarVentaOutput, ApplicationError> crearVenta(
            RegistrarVentaCommand command
    ) {
        var ventaResult = Venta.crear(
                command.sucursalId(),
                command.cajaId(),
                command.vendedorId(),
                command.clienteId(),
                toDomainItems(command.items()),
                toDomainPayments(command.pagos())
        );

        return ventaResult.fold(
                venta -> {
                    ventas.save(venta);
                    events.publishAll(venta.pullDomainEvents());
                    return Result.success(new RegistrarVentaOutput(
                            venta.id(), venta.version(), venta.estado()
                    ));
                },
                error -> Result.failure(VentaErrorMapper.toApplication(error))
        );
    }
}
```

El ejemplo es deliberadamente independiente de Spring. El ensamblaje registra el handler como bean mediante `@Bean` en `VentasModuleConfiguration`.

La reserva o descuento de stock requiere una decisión explícita:

- Si la venta no puede confirmarse sin stock, usar un puerto sincrónico y una transacción local cuidadosamente diseñada.
- Si los contextos se separan físicamente, usar una saga/process manager con reservas, compensaciones e idempotencia.

No se debe asumir atomicidad distribuida.

## 23. Eventos de dominio, módulo e integración

### 23.1. Tres niveles de eventos

| Evento | Visibilidad | Contenido |
|---|---|---|
| Dominio | Interno al agregado/contexto | Expresa un hecho rico del modelo |
| Módulo | API pública dentro del monolito | Contrato pequeño y estable entre contextos |
| Integración | Exterior del sistema | Contrato versionado, serializable e idempotente |

No se publica una entidad de dominio o JPA como evento.

### 23.2. Spring Modulith

Spring Modulith se utiliza para:

- Detectar y verificar módulos lógicos.
- Definir APIs públicas y paquetes internos.
- Validar dependencias permitidas.
- Probar módulos de forma aislada.
- Documentar el grafo de módulos.
- Mantener un registro durable de publicaciones.
- Externalizar eventos posteriormente mediante outbox.

Ejemplo de módulo explícito:

```java
@org.springframework.modulith.ApplicationModule(
        displayName = "Ventas",
        allowedDependencies = {
                "clientes::api",
                "inventario::api",
                "organizacion::api",
                "pagos::api"
        }
)
package pe.com.boticas.ventas;
```

La estrategia recomendada es `explicitly-annotated`, para que `shared-*` no sea detectado accidentalmente como contexto de negocio.

### 23.3. Entrega durable

Para efectos secundarios no críticos dentro de la transacción principal:

```text
Venta confirmada
    ├── persistir venta
    ├── registrar publicación durable en la misma transacción
    └── commit
           ├── finanzas procesa evento
           ├── notificaciones procesa evento
           └── integración externa reintenta si falla
```

Cada consumidor debe:

- Ser idempotente.
- Registrar el identificador del evento procesado.
- Tolerar reintentos y duplicados.
- Tener política de backoff y dead-letter/revisión manual.
- Emitir métricas de retraso y fallos.

## 24. Spring Modulith y Gradle multimódulo

El proyecto utiliza dos niveles complementarios:

```text
Gradle subproject -> aislamiento físico de compilación y dependencias
Spring Modulith   -> verificación lógica, eventos, documentación y pruebas
```

La clase principal debe ubicarse en el paquete raíz:

```text
bootstrap-app/src/main/java/pe/com/boticas/ErpBoticasApplication.java
```

Los bounded contexts se ubican debajo de `pe.com.boticas`. Cada uno declara `@ApplicationModule` en su `package-info.java`.

Prueba estructural mínima:

```java
class ModulithArchitectureTest {

    @Test
    void verifiesModuleBoundaries() {
        ApplicationModules.of(ErpBoticasApplication.class).verify();
    }
}
```

`@ApplicationModuleTest` se usa para levantar un contexto individual en modo `STANDALONE`, `DIRECT_DEPENDENCIES` o `ALL_DEPENDENCIES`, según el alcance real de la prueba.

## 25. Persistencia

### 25.1. Write model

- JPA se limita a `adapter/out/persistence/write`.
- Las entidades JPA no son agregados de dominio.
- Un mapper explícito convierte dominio ↔ persistencia.
- Las asociaciones entre contextos se guardan como identificadores.
- `@Version` controla concurrencia optimista donde aplique.
- Bloqueo pesimista se reserva para secciones cortas y de alta contención, como correlativos o stock.

### 25.2. Read model

- SQL explícito y DTO inmutable.
- Paginación obligatoria en colecciones.
- Índices guiados por consultas reales.
- El filtro de `empresa_id` y alcance organizacional es obligatorio.
- Las consultas no devuelven entidades JPA.

### 25.3. Migraciones

```text
bootstrap-app/src/main/resources/db/migration/
├── shared/
├── security/
├── organizacion/
├── catalogo/
├── inventario/
└── ventas/
```

Cada cambio de esquema se agrega como una nueva migración Flyway. Una migración aplicada no se edita. En producción se desactiva la creación automática de esquemas por Hibernate y se utiliza `ddl-auto: validate`.

## 26. Seguridad transversal sin contaminar el dominio

```text
JWT Filter
   -> AuthenticatedPrincipal
      -> CurrentActorPort
         -> AuthorizationPolicy
            -> Command/Query Handler
```

Reglas:

- El adaptador de seguridad valida firma, emisor, audiencia, expiración y sesión.
- Application valida permisos y alcance de empresa/sucursal para cada caso de uso.
- El dominio recibe un `ActorId` o decisión ya validada cuando la identidad sea relevante.
- Nunca se confía únicamente en permisos ocultos en el frontend.
- Las queries también aplican autorización.
- Los logs no incluyen tokens, contraseñas, datos de tarjeta ni receta médica.

## 27. API y traducción de resultados

El contrato de dominio `Result` se traduce en el adaptador REST a `ProblemDetail` compatible con RFC 9457.

Ejemplo de fallo:

```json
{
  "type": "https://api.boticas.pe/problems/stock-insuficiente",
  "title": "Stock insuficiente",
  "status": 409,
  "detail": "No existe stock disponible para completar la venta.",
  "instance": "/v1/ventas/commands/018f...",
  "code": "INV-STOCK-001",
  "correlationId": "b59d...",
  "errors": []
}
```

Reglas:

- `code` es estable y apto para consumidores.
- `detail` puede localizarse y no se usa para tomar decisiones automáticas.
- `correlationId` permite soporte y trazabilidad.
- Nunca se exponen stack traces, SQL o secretos.
- La validación de campos utiliza una colección estructurada.

## 28. Observabilidad

Toda operación debe propagar:

```text
correlation_id
trace_id
span_id
command_id o query_id
actor_id
empresa_id
sucursal_id
module
operation
result_code
duration_ms
```

Métricas mínimas:

- Latencia y tasa de error por caso de uso.
- Conflictos de concurrencia.
- Reservas de stock expiradas.
- Eventos pendientes, fallidos y reintentados.
- Tiempo de respuesta de SUNAT, DIGEMID y pasarelas.
- Errores por código de negocio.

La observabilidad se implementa en decoradores de command/query handlers y adaptadores, no dentro de agregados.

## 29. Estrategia de pruebas

| Nivel | Qué valida | Herramienta sugerida |
|---|---|---|
| Value object | Construcción e invariantes | JUnit 5 |
| Aggregate | Transiciones y eventos | JUnit 5, sin Spring |
| Command handler | Orquestación y resultados | JUnit 5 + fakes/mocks de puertos |
| Query handler | Mapeo y filtros | JUnit 5 |
| Persistence adapter | SQL, JPA, locks e índices | Testcontainers + PostgreSQL |
| REST adapter | Contratos y RFC 9457 | MockMvc |
| Módulo | Wiring y eventos | `@ApplicationModuleTest` |
| Arquitectura | Límites y dependencias | Spring Modulith + ArchUnit |
| Flujo crítico | Venta, pago, stock y comprobante | Prueba de integración desde bootstrap |
| Concurrencia | Stock, reservas, caja y correlativos | Testcontainers con ejecución paralela |

Quality gates obligatorios:

```text
./gradlew test
./gradlew integrationTest
./gradlew architectureTest
./gradlew check
```

## 30. Decisiones de consistencia

| Operación | Consistencia | Estrategia |
|---|---|---|
| Modificar un agregado | Fuerte | Transacción PostgreSQL |
| Reservar stock para venta | Fuerte | Operación atómica con versión o lock |
| Generar correlativo | Fuerte | Secuencia o actualización bloqueada |
| Notificar cliente | Eventual | Evento durable e idempotente |
| Actualizar dashboard | Eventual aceptable | Proyección de lectura |
| Enviar comprobante a SUNAT | Reintento controlado | Estado local + evento/outbox |
| Confirmar pago por webhook | Fuerte e idempotente | Clave externa única + transición válida |

Una operación remota nunca forma parte de una supuesta transacción ACID distribuida.

## 31. Reglas automatizables de arquitectura

1. `domain..` solo depende de Java y `shared-kernel`.
2. `application..` no depende de `adapter..`.
3. `adapter.in..` solo invoca puertos de entrada.
4. `adapter.out..` implementa puertos de salida.
5. Ningún módulo importa `..internal..` de otro módulo.
6. Las entidades JPA permanecen en adaptadores de persistencia.
7. Los controllers no acceden a repositorios.
8. Los command handlers no retornan entidades de dominio.
9. Las queries no modifican estado.
10. Solo `bootstrap-app` contiene `@SpringBootApplication`.
11. No existen ciclos entre subproyectos Gradle.
12. Los eventos públicos pertenecen al paquete `api.event`.

## 32. Antipatrones que deben evitarse

- `shared-kernel` convertido en depósito de utilidades.
- Una interfaz por cada clase sin un límite arquitectónico real.
- CRUD genérico para agregados con reglas distintas.
- Controllers con reglas de negocio.
- Entidades JPA expuestas como respuesta REST.
- Commands llamados `CreateEntity` que solo copian DTO a tabla.
- Usar CQRS como sinónimo de Kafka o Event Sourcing.
- Publicar eventos antes de confirmar la transacción.
- Consumidores de eventos no idempotentes.
- `Result` que retorna `null` en una de sus ramas.
- Capturar todas las excepciones y convertirlas en errores de negocio.
- Transacciones abiertas durante llamadas HTTP externas.
- Acceso directo a tablas de otro bounded context.
- Herencia desde una clase `BaseService` con dependencias comunes.

## 33. Secuencia de implementación

```text
1. backend-parent + build-logic + catálogo de versiones
2. shared-kernel mínimo: Result, errores, identificadores, reloj
3. bootstrap-app + configuración + observabilidad
4. Spring Modulith + prueba de límites
5. security + organizacion + identidad mínima de trabajador
6. catalogo + farmacia
7. inventario con pruebas de concurrencia
8. compras y recepción
9. ventas POS con CQRS y transacciones
10. pagos y comprobantes mediante adaptadores
11. eventos durables, notificaciones y finanzas
12. proyecciones avanzadas solo después de medir consultas
```

Cada módulo debe entregar primero un corte vertical pequeño: dominio, command, query, persistencia, API, seguridad y pruebas. No se crean todas las capas vacías por adelantado.

## 34. Referencias primarias y oficiales

- [DDD Reference — Eric Evans](https://www.domainlanguage.com/ddd/reference/)
- [The Clean Architecture — Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Hexagonal Architecture original — Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture)
- [CQRS Pattern — Microsoft Azure Architecture Center](https://learn.microsoft.com/en-us/azure/architecture/patterns/cqrs)
- [Spring Modulith Fundamentals](https://docs.spring.io/spring-modulith/reference/fundamentals.html)
- [Spring Modulith Events](https://docs.spring.io/spring-modulith/reference/events.html)
- [Spring Modulith Module Testing](https://docs.spring.io/spring-modulith/reference/testing.html)
- [Gradle Multi-Project Builds](https://docs.gradle.org/current/userguide/multi_project_builds.html)
- [Gradle Java Compatibility](https://docs.gradle.org/current/userguide/compatibility.html)
- [Spring Boot System Requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Framework Declarative Transactions](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html)
- [Spring MVC Problem Details](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [RFC 9457 — Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457.html)
