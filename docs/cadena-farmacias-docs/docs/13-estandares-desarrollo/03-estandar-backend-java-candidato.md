# STD-FAR-003 — Estándar Backend / Java Candidato

## 1. Estado

`CONDICIONAL`: aplica únicamente si el ADR de stack selecciona Java moderno para el backend.

No establece por sí mismo que Java 21 sea obligatorio.

## 2. Estructura

Organización por módulo de negocio, no por capas globales:

```text
modules/
  inventory/
    domain/
    application/
    infrastructure/
  retail/
    domain/
    application/
    infrastructure/
```

## 3. Lambdas

Java define lambdas como forma concisa de expresar instancias de interfaces funcionales. [REF-57]

Usarlas para comportamiento local y legible:

```java
items.stream()
     .filter(Item::isActive)
     .map(Item::id)
     .toList();
```

No esconder reglas relevantes en expresiones anónimas extensas. Preferir:

```java
.filter(vendibilityPolicy::isSellable)
```

frente a una lambda de múltiples condiciones regulatorias.

## 4. Streams

La API `Stream` requiere que los comportamientos usados en pipelines sean no-interferentes y, normalmente, stateless. [REF-58]

Estándar:

- usar Streams para transformación/filtrado/agregación legible;
- no modificar la fuente dentro del pipeline;
- no forzar `parallelStream()` sin benchmark;
- evitar pipelines largos difíciles de depurar;
- no usar Streams para lógica transaccional compleja.

## 5. Records

Los records son carriers transparentes y superficialmente inmutables. [REF-59]

Candidatos:

- Commands;
- Queries;
- DTOs internos;
- Domain Events;
- Value Objects simples cuando sus invariantes lo permitan.

No convertir automáticamente Aggregate Roots JPA en records.

## 6. Sealed types

Sealed classes/interfaces pueden representar jerarquías cerradas cuando el dominio realmente es exhaustivo. [REF-60]

Ejemplo candidato:

```text
Result<T>
  ├─ Success<T>
  └─ Failure<T>
```

## 7. Optional

`Optional` se orienta principalmente a retornos donde existe ausencia legítima de valor. [REF-61]

No usar por defecto:

- como campo persistente;
- en cada parámetro;
- como sustituto de una regla de dominio.

## 8. Excepciones vs Result

- errores esperables del negocio → `Result/DomainError`;
- errores técnicos inesperados → excepción;
- no `catch (Exception)` para silenciar fallos;
- no usar excepción para flujo ordinario de negocio.

## 9. Inmutabilidad

Value Objects y Commands deben preferir inmutabilidad. Las mutaciones del agregado deben realizarse mediante métodos que protejan invariantes.

## 10. Framework

Annotations/framework no deben invadir innecesariamente el dominio. La decisión final sobre Spring Boot, Spring Modulith, JPA/JDBC, MapStruct u otros se toma por ADR/toolchain.
