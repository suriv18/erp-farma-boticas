# STD-FAR-001 — Principios de Diseño

## 1. DDD antes que CRUD

La estructura del código debe preservar el lenguaje ubicuo y los límites del dominio. No se modela el negocio como una colección de `GenericService<T>` y CRUD genérico.

## 2. SOLID

### SRP

Una unidad debe tener una responsabilidad coherente. Evitar servicios como `FarmaciaService` que mezclen catálogo, inventario, venta, CPE y reportes.

### OCP

Reglas variables deben poder evolucionar sin cadenas crecientes de `if/else`. Ejemplos: selección de lote, modalidad CPE, proveedor de pagos, políticas de precio.

### LSP

Un adapter que implementa un puerto debe cumplir el mismo contrato observable. Cambiar `SunatAdapter` por `PseAdapter` no debe alterar las garantías definidas por `FiscalPort`.

### ISP

Preferir puertos/casos de uso pequeños y específicos frente a interfaces gigantes.

### DIP

El dominio/aplicación dependen de abstracciones propias; infraestructura implementa los puertos. El dominio no depende de framework, JPA, HTTP, SUNAT SDK o proveedor cloud.

## 3. GRASP

Se aplican especialmente:

- Information Expert;
- Creator;
- Controller;
- Low Coupling;
- High Cohesion;
- Polymorphism;
- Indirection;
- Protected Variations;
- Pure Fabrication cuando una responsabilidad no pertenece naturalmente a una entidad.

Ejemplo: `Venta.confirmar()` debe proteger invariantes de Venta; el controller no decide vendibilidad de un lote.

## 4. KISS

Preferir la solución más simple que preserve las reglas y atributos de calidad.

## 5. YAGNI

No introducir por anticipado:

- microservicios;
- Event Sourcing;
- broker externo;
- Kubernetes;
- sharding;
- saga distribuida;
- CQRS con dos bases físicas.

## 6. DRY, sin falsa abstracción

Evitar duplicación conceptual, pero no fusionar reglas diferentes solo porque el código se parece. `Dispensación`, `Venta` y `CPE` no se convierten en una única abstracción por compartir datos.

## 7. Composición sobre herencia

La herencia se reserva para relaciones realmente estables. Policies, Strategies y composición son preferibles para comportamiento variable.

## 8. Nombres

Clases, comandos, eventos y métodos deben usar el lenguaje ubicuo:

- `ConfirmarVenta`;
- `DispensarPrescripcion`;
- `BloquearLote`;
- `AbrirCasoRecall`;

no `processData()`, `executeOperation()` o `GenericManager`.
