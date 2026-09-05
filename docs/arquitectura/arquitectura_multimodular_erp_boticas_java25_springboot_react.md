# Arquitectura multimodular ERP Boticas Perú

> **Estado: LEGADO / REFERENCIA HISTÓRICA.** La fuente canónica vigente es
> [`docs/cadena-farmacias-docs/docs/00-gobierno/01-fuente-unica-verdad.md`](../cadena-farmacias-docs/docs/00-gobierno/01-fuente-unica-verdad.md).
> El stack implementado fue ratificado por ADR-010, pero alcance, módulos y seguridad se gobiernan
> desde la documentación canónica.

**Proyecto:** ERP para cadena de boticas en Perú  
**Tipo de solución:** Monolito modular multimódulo preparado para evolucionar a microservicios  
**Backend:** Java 25 + Spring Boot 4.x + Gradle + PostgreSQL + configuración YAML  
**Frontend:** React + Vite + TypeScript  
**Mobile:** React Native / Expo o PWA con React, según el alcance de la app móvil  
**Base de datos:** PostgreSQL  
**Patrones:** Clean Architecture + DDD + Ports and Adapters + CQRS + Result Pattern  
**Canales:** App Web ERP, App Web POS, App Web Ecommerce, App Móvil Cliente, App Móvil Inventario, App Móvil Repartidor, App Móvil Supervisor

---

## 1. Objetivo de la arquitectura

El sistema debe soportar el core de una cadena de boticas en Perú, incluyendo venta de productos farmacéuticos, perfumería, cosméticos, cuidado personal, prendas, dispositivos médicos, productos sanitarios y otras categorías comerciales.

La arquitectura debe permitir:

- Separar claramente reglas de negocio, casos de uso e infraestructura.
- Trabajar por módulos de negocio independientes.
- Reutilizar lógica para app web, POS, ecommerce y app móvil.
- Mantener trazabilidad, auditoría y seguridad RBAC.
- Soportar POS, ecommerce, delivery, inventario, compras, ventas, finanzas, RR. HH., CRM y CMR.
- Permitir que en el futuro algunos módulos puedan convertirse en microservicios sin reescribir todo el sistema.
- Trabajar con Java 25, Spring Boot, Gradle, archivos `application.yml` y React.

---

## 2. Decisión arquitectónica principal

Se recomienda iniciar con un **monolito modular multimódulo**.

No se recomienda iniciar directamente con microservicios porque el ERP tiene muchas reglas compartidas, transacciones internas y dependencias fuertes entre inventario, ventas, caja, comprobantes, clientes y productos. Un monolito modular permite mantener orden, bajo acoplamiento y facilidad de despliegue inicial.

Cuando el negocio crezca, algunos módulos pueden separarse como microservicios, por ejemplo:

- Seguridad e identidad.
- Inventario.
- Ventas/ecommerce.
- Pagos.
- Notificaciones.
- Integraciones SUNAT/DIGEMID.

---

## 3. Stack tecnológico definido

### 3.1. Backend

```text
Lenguaje       : Java 25
Framework      : Spring Boot 4.x
Build tool     : Gradle 9.x
Config         : application.yml por perfiles
Base de datos  : PostgreSQL
ORM            : Spring Data JPA / Hibernate
Migraciones    : Flyway
Seguridad      : Spring Security + JWT + RBAC
Documentación  : OpenAPI / Swagger
Testing        : JUnit 5, Mockito, Testcontainers
Observabilidad : Actuator, Micrometer, logs estructurados
```

### 3.2. Frontend web

```text
Framework      : React
Build tool     : Vite
Lenguaje       : TypeScript
Routing        : React Router
Estado servidor: TanStack Query / React Query
Estado local   : Zustand o Context API
Formularios    : React Hook Form
Validación     : Zod
UI             : Tailwind CSS o Material UI
Testing        : Vitest + React Testing Library
```

### 3.3. App móvil

Para la app móvil existen dos rutas válidas:

```text
Opción A: React Native / Expo
- Recomendado si se requiere app móvil instalada en Android/iOS.
- Útil para repartidores, inventario, escaneo de códigos, GPS y notificaciones push.

Opción B: PWA con React
- Recomendado si se quiere una sola app web instalable.
- Útil para POS móvil, supervisor, inventario simple o ventas rápidas.
```

Para el proyecto se recomienda:

```text
ERP Backoffice       -> React Web
POS Web              -> React Web / PWA
Ecommerce            -> React Web
App Cliente          -> React Native / Expo o PWA
App Repartidor       -> React Native / Expo
App Inventario       -> React Native / Expo o PWA
App Supervisor       -> React Web responsive / PWA
```

---

## 4. Principios base

### 4.1. Clean Architecture

Cada módulo debe dividirse en capas:

```text
Domain         -> Reglas puras del negocio
Application    -> Casos de uso, comandos, consultas y puertos
Infrastructure -> Persistencia, integraciones, seguridad técnica
Interfaces     -> REST controllers, DTOs, validaciones de entrada
```

La dependencia siempre debe ir hacia adentro:

```text
Interfaces -> Application -> Domain
Infrastructure -> Application / Domain
Domain no depende de Spring, JPA, PostgreSQL ni frameworks externos
```

### 4.2. DDD: Domain Driven Design

El sistema se organiza por **bounded contexts** o contextos de negocio:

- Seguridad.
- Organización.
- Catálogo.
- Farmacia.
- Inventario.
- Compras.
- Ventas.
- Finanzas.
- RR. HH.
- CRM.
- CMR.
- App/Omnicanal.
- Logística.
- Pagos.
- Notificaciones.
- Integraciones.

Cada contexto debe tener sus propias entidades, agregados, value objects, repositorios y servicios de dominio.

### 4.3. Ports and Adapters

Los casos de uso no deben depender directamente de PostgreSQL, APIs externas, correo, WhatsApp, pasarelas de pago, SUNAT, DIGEMID o servicios externos.

Se usarán puertos en `application` y adaptadores en `infrastructure`.

Ejemplo:

```text
VentaUseCase -> ProductoStockPort -> InventarioAdapterPostgres
VentaUseCase -> FacturacionPort   -> SunatAdapter
VentaUseCase -> PagoPort          -> NiubizAdapter / CulqiAdapter
VentaUseCase -> NotificacionPort  -> EmailAdapter / WhatsAppAdapter
```

### 4.4. CQRS

Se separan operaciones de escritura y lectura:

```text
Command -> Cambia el estado del sistema
Query   -> Consulta información sin modificar datos
```

Ejemplos:

```text
RegistrarVentaCommand
AnularVentaCommand
RegistrarCompraCommand
ActualizarStockCommand
CrearClienteCommand

BuscarProductosQuery
ListarVentasPorSucursalQuery
ObtenerStockPorLoteQuery
ConsultarClienteDetalleQuery
```

### 4.5. Result Pattern

Los casos de uso no deben devolver `null` ni lanzar excepciones para errores esperados del negocio.

Deben devolver un resultado controlado:

```text
Result.success(data)
Result.failure(error)
```

Ejemplos de errores de negocio:

- Stock insuficiente.
- Producto vencido.
- Usuario sin permiso.
- Cliente no encontrado.
- Caja cerrada.
- Venta no anulable.
- Comprobante ya emitido.
- Lote bloqueado.

---

## 5. Estructura general del repositorio

Se recomienda un repositorio principal para backend y frontend, organizado como monorepo técnico:

```text
erp-boticas-peru/
│
├── backend/
│   ├── settings.gradle
│   ├── build.gradle
│   ├── gradle.properties
│   ├── gradlew
│   ├── gradlew.bat
│   ├── gradle/
│   │   └── wrapper/
│   │
│   ├── bootstrap-app/
│   │   └── Aplicación principal Spring Boot
│   │
│   ├── shared-kernel/
│   │   └── Result, errores, auditoría, eventos, value objects base
│   │
│   ├── security/
│   ├── organizacion/
│   ├── catalogo/
│   ├── farmacia/
│   ├── inventario/
│   ├── compras/
│   ├── ventas/
│   ├── finanzas/
│   ├── rrhh/
│   ├── crm/
│   ├── cmr/
│   ├── app-channel/
│   ├── logistica/
│   ├── pagos/
│   ├── notificaciones/
│   └── integraciones/
│
├── frontend/
│   ├── apps/
│   │   ├── erp-web/
│   │   ├── pos-web/
│   │   ├── ecommerce-web/
│   │   ├── cliente-mobile/
│   │   ├── inventario-mobile/
│   │   └── repartidor-mobile/
│   │
│   ├── packages/
│   │   ├── ui/
│   │   ├── api-client/
│   │   ├── auth/
│   │   ├── validations/
│   │   └── shared-types/
│   │
│   └── package.json
│
├── database/
│   ├── migrations/
│   │   └── V1__init_erp_boticas_postgres_revisado.sql
│   └── seeds/
│
├── docs/
│   ├── arquitectura/
│   ├── base-datos/
│   ├── api/
│   └── decisiones-adr/
│
└── README.md
```

---

## 6. Estructura Gradle multimódulo del backend

### 6.1. `settings.gradle`

```groovy
pluginManagement {
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

rootProject.name = 'erp-boticas-backend'

include 'bootstrap-app'
include 'shared-kernel'

include 'security'
include 'organizacion'
include 'catalogo'
include 'farmacia'
include 'inventario'
include 'compras'
include 'ventas'
include 'finanzas'
include 'rrhh'
include 'crm'
include 'cmr'
include 'app-channel'
include 'logistica'
include 'pagos'
include 'notificaciones'
include 'integraciones'
```

### 6.2. `build.gradle` raíz

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.6' apply false
    id 'io.spring.dependency-management' version '1.1.7' apply false
}

allprojects {
    group = 'pe.com.boticas'
    version = '1.0.0-SNAPSHOT'
}

subprojects {
    apply plugin: 'java'
    apply plugin: 'io.spring.dependency-management'

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation 'org.junit.jupiter:junit-jupiter'
        testImplementation 'org.mockito:mockito-core'
    }

    test {
        useJUnitPlatform()
    }
}
```

### 6.3. `bootstrap-app/build.gradle`

```groovy
plugins {
    id 'org.springframework.boot'
}

dependencies {
    implementation project(':shared-kernel')

    implementation project(':security')
    implementation project(':organizacion')
    implementation project(':catalogo')
    implementation project(':farmacia')
    implementation project(':inventario')
    implementation project(':compras')
    implementation project(':ventas')
    implementation project(':finanzas')
    implementation project(':rrhh')
    implementation project(':crm')
    implementation project(':cmr')
    implementation project(':app-channel')
    implementation project(':logistica')
    implementation project(':pagos')
    implementation project(':notificaciones')
    implementation project(':integraciones')

    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'

    runtimeOnly 'org.postgresql:postgresql'

    implementation 'org.flywaydb:flyway-core'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
}
```

### 6.4. `ventas/build.gradle`

Ejemplo de módulo de negocio:

```groovy
dependencies {
    implementation project(':shared-kernel')
    implementation project(':catalogo')
    implementation project(':inventario')
    implementation project(':clientes') // Solo si se separa cliente de CRM

    implementation 'org.springframework:spring-context'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
}
```

> Nota: Si se desea máxima independencia entre módulos, `ventas` no debe depender directamente de `catalogo` ni `inventario`; debe depender de puertos publicados por esos módulos. Para una primera versión, puede aceptarse dependencia controlada entre módulos, siempre evitando acoplamiento a entidades JPA externas.

---

## 7. Estructura interna de cada módulo backend

Cada módulo debe seguir la misma estructura:

```text
ventas/
└── src/main/java/pe/com/boticas/ventas/
    │
    ├── domain/
    │   ├── model/
    │   │   ├── Venta.java
    │   │   ├── DetalleVenta.java
    │   │   ├── PagoVenta.java
    │   │   └── VentaEstado.java
    │   │
    │   ├── valueobject/
    │   │   ├── VentaId.java
    │   │   ├── Monto.java
    │   │   └── NumeroComprobante.java
    │   │
    │   ├── repository/
    │   │   └── VentaRepository.java
    │   │
    │   ├── service/
    │   │   └── VentaDomainService.java
    │   │
    │   └── event/
    │       └── VentaRegistradaEvent.java
    │
    ├── application/
    │   ├── command/
    │   │   ├── RegistrarVentaCommand.java
    │   │   └── AnularVentaCommand.java
    │   │
    │   ├── query/
    │   │   ├── BuscarVentasQuery.java
    │   │   └── ObtenerVentaDetalleQuery.java
    │   │
    │   ├── handler/
    │   │   ├── RegistrarVentaHandler.java
    │   │   ├── AnularVentaHandler.java
    │   │   └── ObtenerVentaDetalleHandler.java
    │   │
    │   ├── port/in/
    │   │   ├── RegistrarVentaUseCase.java
    │   │   └── AnularVentaUseCase.java
    │   │
    │   ├── port/out/
    │   │   ├── StockPort.java
    │   │   ├── CajaPort.java
    │   │   ├── ClientePort.java
    │   │   ├── ComprobantePort.java
    │   │   └── VentaPersistencePort.java
    │   │
    │   └── dto/
    │       ├── VentaResponse.java
    │       └── VentaResumenResponse.java
    │
    ├── infrastructure/
    │   ├── persistence/
    │   │   ├── entity/
    │   │   │   ├── VentaJpaEntity.java
    │   │   │   └── DetalleVentaJpaEntity.java
    │   │   ├── repository/
    │   │   │   └── SpringDataVentaRepository.java
    │   │   ├── mapper/
    │   │   │   └── VentaPersistenceMapper.java
    │   │   └── adapter/
    │   │       └── VentaPersistenceAdapter.java
    │   │
    │   └── integration/
    │       ├── SunatComprobanteAdapter.java
    │       └── InventarioStockAdapter.java
    │
    └── interfaces/
        └── rest/
            ├── VentaController.java
            ├── request/
            │   ├── RegistrarVentaRequest.java
            │   └── AnularVentaRequest.java
            └── mapper/
                └── VentaRestMapper.java
```

---

## 8. Configuración YAML recomendada

### 8.1. `application.yml`

```yaml
spring:
  application:
    name: erp-boticas-backend

  profiles:
    active: dev

  datasource:
    url: jdbc:postgresql://localhost:5432/erp_boticas
    username: erp_boticas_user
    password: erp_boticas_pass
    driver-class-name: org.postgresql.Driver

  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
        jdbc:
          time_zone: America/Lima

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

server:
  port: 8080
  servlet:
    context-path: /api

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when_authorized

security:
  jwt:
    issuer: erp-boticas
    access-token-expiration-minutes: 30
    refresh-token-expiration-days: 7
    secret: ${JWT_SECRET}

app:
  timezone: America/Lima
  audit:
    enabled: true
  cors:
    allowed-origins:
      - http://localhost:5173
      - http://localhost:5174
      - http://localhost:5175
```

### 8.2. `application-dev.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/erp_boticas_dev
    username: postgres
    password: postgres

  jpa:
    show-sql: true

logging:
  level:
    pe.com.boticas: DEBUG
    org.hibernate.SQL: DEBUG
```

### 8.3. `application-prod.yml`

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  jpa:
    show-sql: false

logging:
  level:
    root: INFO
    pe.com.boticas: INFO

security:
  jwt:
    secret: ${JWT_SECRET}
```

---

## 9. Result Pattern en Java 25

Se recomienda definir el `Result` en `shared-kernel`.

```java
package pe.com.boticas.shared.result;

import java.util.Objects;

public sealed interface Result<T> permits Result.Success, Result.Failure {

    boolean isSuccess();

    default boolean isFailure() {
        return !isSuccess();
    }

    T value();

    ErrorDetail error();

    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    static <T> Result<T> failure(ErrorDetail error) {
        return new Failure<>(error);
    }

    record Success<T>(T value) implements Result<T> {
        public Success {
            Objects.requireNonNull(value, "value no puede ser null");
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public ErrorDetail error() {
            return null;
        }
    }

    record Failure<T>(ErrorDetail error) implements Result<T> {
        public Failure {
            Objects.requireNonNull(error, "error no puede ser null");
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public T value() {
            return null;
        }
    }
}
```

```java
package pe.com.boticas.shared.result;

public record ErrorDetail(
        String code,
        String message,
        String field
) {
    public static ErrorDetail of(String code, String message) {
        return new ErrorDetail(code, message, null);
    }

    public static ErrorDetail field(String code, String message, String field) {
        return new ErrorDetail(code, message, field);
    }
}
```

---

## 10. Ejemplo CQRS: registrar venta POS

### 10.1. Command

```java
package pe.com.boticas.ventas.application.command;

import java.math.BigDecimal;
import java.util.List;

public record RegistrarVentaCommand(
        Long sucursalId,
        Long cajaId,
        Long usuarioId,
        Long clienteId,
        String tipoComprobante,
        String canalVenta,
        List<Item> items,
        List<Pago> pagos
) {
    public record Item(
            Long productoId,
            Long loteId,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal descuento
    ) {}

    public record Pago(
            String medioPago,
            BigDecimal monto,
            String referencia
    ) {}
}
```

### 10.2. Use Case

```java
package pe.com.boticas.ventas.application.port.in;

import pe.com.boticas.shared.result.Result;
import pe.com.boticas.ventas.application.command.RegistrarVentaCommand;
import pe.com.boticas.ventas.application.dto.VentaResponse;

public interface RegistrarVentaUseCase {
    Result<VentaResponse> execute(RegistrarVentaCommand command);
}
```

### 10.3. Handler

```java
package pe.com.boticas.ventas.application.handler;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.boticas.shared.result.ErrorDetail;
import pe.com.boticas.shared.result.Result;
import pe.com.boticas.ventas.application.command.RegistrarVentaCommand;
import pe.com.boticas.ventas.application.dto.VentaResponse;
import pe.com.boticas.ventas.application.port.in.RegistrarVentaUseCase;
import pe.com.boticas.ventas.application.port.out.CajaPort;
import pe.com.boticas.ventas.application.port.out.StockPort;
import pe.com.boticas.ventas.application.port.out.VentaPersistencePort;

@Service
public class RegistrarVentaHandler implements RegistrarVentaUseCase {

    private final StockPort stockPort;
    private final CajaPort cajaPort;
    private final VentaPersistencePort ventaPersistencePort;

    public RegistrarVentaHandler(
            StockPort stockPort,
            CajaPort cajaPort,
            VentaPersistencePort ventaPersistencePort
    ) {
        this.stockPort = stockPort;
        this.cajaPort = cajaPort;
        this.ventaPersistencePort = ventaPersistencePort;
    }

    @Override
    @Transactional
    public Result<VentaResponse> execute(RegistrarVentaCommand command) {

        if (!cajaPort.estaAbierta(command.cajaId(), command.usuarioId())) {
            return Result.failure(ErrorDetail.of("CAJA_CERRADA", "La caja no está abierta para el usuario."));
        }

        for (var item : command.items()) {
            var stockDisponible = stockPort.validarStockDisponible(
                    command.sucursalId(),
                    item.productoId(),
                    item.loteId(),
                    item.cantidad()
            );

            if (stockDisponible.isFailure()) {
                return Result.failure(stockDisponible.error());
            }
        }

        var ventaCreada = ventaPersistencePort.registrar(command);

        if (ventaCreada.isFailure()) {
            return Result.failure(ventaCreada.error());
        }

        return Result.success(ventaCreada.value());
    }
}
```

---

## 11. Seguridad RBAC para backend web y móvil

El modelo de seguridad debe soportar:

```text
usuario -> usuario_rol_sucursal -> rol -> rol_permiso -> permiso
```

El permiso se debe manejar con esta estructura lógica:

```text
modulo:recurso:accion
```

Ejemplos:

```text
ventas:venta:crear
ventas:venta:anular
ventas:caja:abrir
ventas:caja:cerrar
inventario:stock:consultar
inventario:ajuste:crear
compras:orden-compra:aprobar
rrhh:trabajador:crear
crm:cliente:consultar
security:usuario:asignar-rol
```

### 11.1. Control por canal

No todos los usuarios pueden acceder a todos los canales:

```text
WEB_ERP       -> administrador, gerente, contador, supervisor
WEB_POS       -> cajero, químico farmacéutico, vendedor
WEB_ECOMMERCE -> cliente externo
MOBILE_CLIENTE -> cliente externo
MOBILE_REPARTIDOR -> repartidor
MOBILE_INVENTARIO -> almacenero, supervisor
```

### 11.2. Relación trabajador - usuario

El trabajador pertenece a RR. HH. El usuario pertenece a seguridad.

```text
rrhh.trabajador.usuario_id -> security.usuario.usuario_id
```

Reglas:

- No todo trabajador necesita usuario.
- Todo usuario interno debe estar asociado a un trabajador activo.
- Un cliente de ecommerce o app móvil no debe registrarse como trabajador.
- Los permisos se asignan al usuario, no directamente al trabajador.
- La información laboral se controla en RR. HH.

---

## 12. Auditoría técnica y de negocio

### 12.1. Campos auditables básicos

En tablas maestras y transaccionales se recomienda:

```text
created_at
created_by
updated_at
updated_by
```

### 12.2. Eliminación lógica

Solo usar en tablas donde realmente aplica baja lógica:

```text
deleted_at
deleted_by
```

Aplica para:

- Usuarios.
- Roles.
- Clientes.
- Proveedores.
- Productos.
- Categorías.
- Marcas.
- Sucursales.
- Almacenes.
- Promociones.
- Campañas.
- Configuraciones.

No aplica para:

- Ventas.
- Detalle de ventas.
- Pagos.
- Kardex.
- Movimientos de inventario.
- Comprobantes electrónicos.
- Compras.
- Asientos contables.
- Logs de integración.
- Sesiones.

En transacciones se debe usar estado:

```text
REGISTRADO
EMITIDO
ANULADO
CANCELADO
RECHAZADO
OBSERVADO
FINALIZADO
```

---

## 13. Frontend React: estructura recomendada

### 13.1. Web ERP

```text
frontend/apps/erp-web/
├── src/
│   ├── app/
│   │   ├── router.tsx
│   │   ├── providers.tsx
│   │   └── main.tsx
│   │
│   ├── modules/
│   │   ├── security/
│   │   ├── organizacion/
│   │   ├── catalogo/
│   │   ├── inventario/
│   │   ├── compras/
│   │   ├── ventas/
│   │   ├── finanzas/
│   │   ├── rrhh/
│   │   ├── crm/
│   │   └── reportes/
│   │
│   ├── shared/
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── layouts/
│   │   ├── guards/
│   │   └── utils/
│   │
│   └── config/
│       └── env.ts
```

### 13.2. POS Web

```text
frontend/apps/pos-web/
├── src/
│   ├── modules/
│   │   ├── caja/
│   │   ├── venta-rapida/
│   │   ├── busqueda-producto/
│   │   ├── comprobante/
│   │   └── cierre-caja/
│   └── shared/
```

### 13.3. Ecommerce Web

```text
frontend/apps/ecommerce-web/
├── src/
│   ├── modules/
│   │   ├── catalogo-publico/
│   │   ├── carrito/
│   │   ├── checkout/
│   │   ├── tracking/
│   │   └── perfil-cliente/
│   └── shared/
```

### 13.4. App móvil

Si se usa React Native / Expo:

```text
frontend/apps/cliente-mobile/
├── src/
│   ├── app/
│   ├── screens/
│   ├── navigation/
│   ├── modules/
│   │   ├── auth/
│   │   ├── productos/
│   │   ├── carrito/
│   │   ├── pedidos/
│   │   ├── recetas/
│   │   └── notificaciones/
│   └── shared/
```

---

## 14. Cliente API compartido para React

Se recomienda crear un paquete común:

```text
frontend/packages/api-client/
```

Ejemplo:

```ts
export type ApiResult<T> = {
  success: boolean;
  data?: T;
  error?: {
    code: string;
    message: string;
    field?: string;
  };
};
```

```ts
export async function apiGet<T>(url: string): Promise<ApiResult<T>> {
  const response = await fetch(`${import.meta.env.VITE_API_URL}${url}`, {
    headers: {
      Authorization: `Bearer ${localStorage.getItem('accessToken') ?? ''}`,
    },
  });

  return response.json();
}
```

---

## 15. Comunicación backend - frontend

### 15.1. Formato estándar de respuesta

Todo endpoint debe responder de forma consistente:

```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-07-14T22:00:00-05:00"
}
```

Error:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "STOCK_INSUFICIENTE",
    "message": "No hay stock suficiente para el producto seleccionado.",
    "field": "items[0].cantidad"
  },
  "timestamp": "2026-07-14T22:00:00-05:00"
}
```

### 15.2. Convención de endpoints

```text
/api/v1/auth/login
/api/v1/auth/refresh-token
/api/v1/security/usuarios
/api/v1/security/roles
/api/v1/catalogo/productos
/api/v1/inventario/stocks
/api/v1/ventas/pos
/api/v1/ventas/pedidos-digitales
/api/v1/compras/ordenes-compra
/api/v1/crm/clientes
/api/v1/rrhh/trabajadores
/api/v1/pagos/intentos
/api/v1/logistica/entregas
```

---

## 16. Módulos de backend y responsabilidades

### 16.1. `shared-kernel`

Responsabilidades:

- `Result<T>`.
- `ErrorDetail`.
- Excepciones técnicas base.
- Auditoría.
- Identificadores base.
- Value objects comunes: `Money`, `Email`, `DocumentoIdentidad`, `Telefono`.
- Eventos de dominio base.

No debe contener reglas específicas de ventas, inventario, farmacia o RR. HH.

### 16.2. `security`

Responsabilidades:

- Login.
- JWT.
- Refresh token.
- Usuarios.
- Roles.
- Permisos.
- Sesiones.
- RBAC.
- Control de acceso por módulo, sucursal, almacén y caja.

### 16.3. `organizacion`

Responsabilidades:

- Empresa.
- Sucursal.
- Almacén.
- Caja.
- Configuración por local.
- Parámetros operativos.

### 16.4. `catalogo`

Responsabilidades:

- Producto/SKU.
- Categoría.
- Marca.
- Unidad de medida.
- Código de barras.
- Precio.
- Presentación.
- Producto activo/inactivo.

### 16.5. `farmacia`

Responsabilidades:

- Principio activo.
- Concentración.
- Forma farmacéutica.
- Condición de venta.
- Producto controlado.
- Receta.
- Reglas sanitarias.
- Información DIGEMID.

### 16.6. `inventario`

Responsabilidades:

- Stock por sucursal, almacén, producto y lote.
- Vencimientos.
- Kardex.
- Transferencias.
- Ajustes.
- Reservas de stock para ecommerce/app.
- Bloqueos de lote.

### 16.7. `compras`

Responsabilidades:

- Proveedores.
- Orden de compra.
- Recepción.
- Documento de compra.
- Costos.
- Cuentas por pagar.

### 16.8. `ventas`

Responsabilidades:

- Venta POS.
- Venta ecommerce.
- Pedido digital.
- Detalle de venta.
- Descuentos.
- Promociones aplicadas.
- Devoluciones.
- Comprobantes.

### 16.9. `finanzas`

Responsabilidades:

- Caja administrativa.
- Caja POS.
- Bancos.
- Cuentas por cobrar.
- Cuentas por pagar.
- Asientos contables.
- Conciliación.

### 16.10. `rrhh`

Responsabilidades:

- Trabajador.
- Contrato.
- Cargo.
- Asignación a sucursal.
- Turno.
- Programación de turno.
- Asistencia.

### 16.11. `crm`

Responsabilidades:

- Cliente.
- Segmentación.
- Fidelización.
- Puntos.
- Campañas.
- Reclamos.
- Cupones.

### 16.12. `cmr`

Responsabilidades:

- Preferencias del cliente.
- Consentimientos.
- Perfilamiento.
- Intereses.
- Autogestión de datos.
- Indicadores de relación cliente-negocio.

### 16.13. `app-channel`

Responsabilidades:

- Cuenta digital de cliente.
- Dispositivos.
- Carrito.
- Sesiones de app.
- Feature flags.
- Versiones de app.

### 16.14. `logistica`

Responsabilidades:

- Delivery.
- Repartidor.
- Tracking.
- Zonas de reparto.
- Estados de entrega.

### 16.15. `pagos`

Responsabilidades:

- Intentos de pago.
- Pasarelas.
- Conciliación.
- Webhooks.
- Medios de pago.

### 16.16. `notificaciones`

Responsabilidades:

- Email.
- SMS.
- WhatsApp.
- Push notifications.
- Plantillas.
- Historial de envío.

### 16.17. `integraciones`

Responsabilidades:

- SUNAT.
- DIGEMID.
- RENIEC.
- Pasarelas externas.
- Logs de integración.
- Reintentos.

---

## 17. Regla de dependencias entre módulos

Reglas generales:

```text
1. Domain no depende de Spring.
2. Application depende de Domain y Shared Kernel.
3. Infrastructure depende de Application y Domain.
4. Interfaces REST depende de Application.
5. Un módulo no debe acceder directamente a tablas de otro módulo.
6. La comunicación entre módulos debe hacerse mediante puertos, eventos o servicios de aplicación expuestos.
7. El bootstrap-app ensambla todos los módulos.
```

Dependencias permitidas inicialmente:

```text
ventas -> inventario mediante StockPort
ventas -> finanzas mediante CajaPort
ventas -> crm mediante ClientePort
ventas -> pagos mediante PagoPort
ventas -> integraciones mediante FacturacionPort
compras -> inventario mediante RecepcionStockPort
app-channel -> ventas mediante PedidoDigitalPort
logistica -> ventas mediante PedidoEntregaPort
notificaciones -> eventos publicados por otros módulos
```

---

## 18. Eventos de dominio recomendados

```text
VentaRegistradaEvent
VentaAnuladaEvent
PagoConfirmadoEvent
ComprobanteEmitidoEvent
StockReservadoEvent
StockDescontadoEvent
LotePorVencerEvent
PedidoDigitalCreadoEvent
PedidoAsignadoRepartidorEvent
PedidoEntregadoEvent
ClienteRegistradoEvent
TrabajadorCreadoEvent
UsuarioBloqueadoEvent
```

En la primera versión, los eventos pueden manejarse de forma interna usando eventos de Spring. En una futura arquitectura de microservicios, pueden publicarse en Kafka o RabbitMQ.

---

## 19. Testing recomendado

### 19.1. Backend

```text
Domain         -> pruebas unitarias puras
Application    -> pruebas de casos de uso con mocks de puertos
Infrastructure -> pruebas con Testcontainers/PostgreSQL
Interfaces     -> pruebas REST con MockMvc o WebTestClient
Security       -> pruebas de permisos por rol y sucursal
```

### 19.2. Frontend

```text
Componentes    -> React Testing Library
Hooks          -> Vitest
Flujos críticos-> Playwright
Validaciones   -> pruebas unitarias de schemas Zod
```

Flujos críticos que deben probarse:

- Login.
- Apertura de caja.
- Venta POS.
- Descuento de stock.
- Emisión de comprobante.
- Pedido ecommerce.
- Pago digital.
- Tracking de delivery.
- Registro de cliente.
- Asignación de rol.

---

## 20. Fases recomendadas de construcción

### Fase 1: Base técnica

- Crear proyecto Gradle multimódulo.
- Configurar Java 25.
- Configurar Spring Boot.
- Configurar PostgreSQL.
- Configurar Flyway.
- Crear `shared-kernel`.
- Crear `Result Pattern`.
- Crear estructura base de módulos.

### Fase 2: Seguridad y organización

- Usuarios.
- Trabajadores.
- Roles.
- Permisos.
- Sucursales.
- Almacenes.
- Cajas.
- Login JWT.

### Fase 3: Catálogo, farmacia e inventario

- Productos.
- Categorías.
- Marcas.
- Lotes.
- Vencimientos.
- Stock.
- Kardex.

### Fase 4: Ventas POS

- Apertura de caja.
- Búsqueda de producto.
- Registro de venta.
- Pago.
- Descuento de stock.
- Comprobante.
- Cierre de caja.

### Fase 5: Compras y reposición

- Proveedores.
- Orden de compra.
- Recepción.
- Actualización de stock.
- Reglas de reposición.

### Fase 6: CRM y CMR

- Clientes.
- Segmentación.
- Puntos.
- Campañas.
- Consentimientos.
- Preferencias.

### Fase 7: Ecommerce y app móvil

- Carrito.
- Pedido digital.
- Reserva de stock.
- Pago online.
- Delivery.
- Tracking.
- Push notifications.

### Fase 8: Finanzas, reportes e integraciones

- Cuentas por cobrar.
- Cuentas por pagar.
- Conciliación.
- Reportes gerenciales.
- Integración SUNAT.
- Integración DIGEMID.

---

## 21. Convenciones de nombres

### 21.1. Java

```text
Paquetes       -> pe.com.boticas.modulo.capa
Clases dominio -> Producto, Venta, Trabajador
Commands       -> RegistrarVentaCommand
Queries        -> BuscarProductosQuery
Handlers       -> RegistrarVentaHandler
Puertos entrada-> RegistrarVentaUseCase
Puertos salida -> StockPort, PagoPort, FacturacionPort
Adaptadores    -> StockPostgresAdapter, SunatAdapter
DTO REST       -> RegistrarVentaRequest, VentaResponse
```

### 21.2. Base de datos

```text
Tablas         -> snake_case
Columnas       -> snake_case
PK             -> nombre_tabla_id
FK             -> tabla_referenciada_id
Esquemas       -> security, catalogo, inventario, ventas, rrhh, crm, etc.
```

### 21.3. React

```text
Componentes    -> PascalCase
Hooks          -> useNombre
Servicios      -> nombreService.ts
Tipos          -> nombre.types.ts
Schemas        -> nombre.schema.ts
Rutas          -> nombre.routes.tsx
```

---

## 22. Consideraciones para app web y app móvil

### 22.1. Reutilización de contratos

El backend debe exponer contratos estables en JSON. El frontend web y móvil deben consumir los mismos endpoints cuando sea posible.

Ejemplo:

```text
GET /api/v1/catalogo/productos?search=paracetamol
GET /api/v1/inventario/stocks?productoId=1&sucursalId=1
POST /api/v1/ventas/pos
POST /api/v1/ventas/pedidos-digitales
```

### 22.2. Autenticación por canal

```text
Usuario interno ERP/POS       -> login de seguridad interna
Cliente ecommerce/app móvil   -> cuenta digital de cliente
Repartidor móvil              -> trabajador + usuario interno limitado
Inventario móvil              -> trabajador + usuario interno limitado
```

### 22.3. Offline parcial

Para app móvil de inventario o repartidor se puede considerar offline parcial:

```text
- Cache local de tareas asignadas.
- Registro temporal de escaneos.
- Sincronización cuando vuelva internet.
- Control de conflictos desde backend.
```

---

## 23. Recomendación final

Para este ERP de boticas, la mejor base es:

```text
Backend:
Java 25 + Spring Boot 4.x + Gradle multimódulo + PostgreSQL + Flyway

Arquitectura:
Clean Architecture + DDD + Ports and Adapters + CQRS + Result Pattern

Frontend:
React + Vite + TypeScript

Mobile:
React Native / Expo para app nativa o PWA con React si se busca menor complejidad inicial
```

Esta estructura permite iniciar ordenadamente como monolito modular, mantener bajo acoplamiento entre áreas del negocio y preparar el sistema para crecer hacia microservicios si la cadena de boticas aumenta en sucursales, ventas digitales, delivery e integraciones externas.

---

## 24. Referencias técnicas

- Spring Boot System Requirements: https://docs.spring.io/spring-boot/system-requirements.html
- Spring Boot 4.0 announcement: https://spring.io/blog/2025/11/20/spring-boot-4-0-0-available-now
- Gradle Java Compatibility: https://docs.gradle.org/current/userguide/compatibility.html
- Gradle 9.1 Java 25 support: https://docs.gradle.org/9.1.0/release-notes.html
- React: Build a React app from Scratch: https://react.dev/learn/build-a-react-app-from-scratch
- Vite Getting Started: https://vite.dev/guide/
