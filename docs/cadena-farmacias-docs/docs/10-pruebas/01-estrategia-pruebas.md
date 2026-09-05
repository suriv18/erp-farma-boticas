# TST-FAR-001 — Estrategia de Pruebas

**Versión:** 0.1  
**Estado:** Borrador trazable  
**Fecha:** 2026-08-30

## 1. Objetivo

Definir cómo se verificará la cadena `RF → RN → CU → CA → prueba`, manteniendo especial atención a integridad de inventario, competencia farmacéutica, fiscalidad, privacidad, idempotencia y trazabilidad.

## 2. Principio

Un endpoint o pantalla que funciona en el escenario feliz no implica cumplimiento del RF. Para cada requisito crítico se evaluarán, según corresponda:

- escenario positivo;
- validaciones negativas;
- seguridad/competencia profesional;
- privacidad;
- histórico/versionado;
- concurrencia;
- idempotencia;
- auditoría;
- integración externa;
- recuperación/reintentos.

## 3. Niveles

### 3.1. Pruebas de dominio

Validarán invariantes puras sin depender de UI, base de datos o servicios externos. Ejemplos futuros: stock vendible, transición de transferencia, bloqueo de lote, validez de una regla de receta, no sobrescritura histórica.

### 3.2. Pruebas de aplicación / casos de uso

Validarán Commands/Use Cases contra los CA de `08-criterios-aceptacion.md`, incluyendo autorización contextual y resultados de negocio.

### 3.3. Pruebas de integración

Prioritarias para:

- reserva/venta concurrente;
- movimientos y saldos de inventario;
- idempotencia de venta/CPE/posting;
- trazabilidad de transferencias;
- privacidad/auditoría;
- persistencia histórica/versionada.

### 3.4. Pruebas de contrato

Se usarán cuando existan contratos formales con SUNAT, ERP, pagos, Observatorio, e-commerce u otras integraciones. La fuente oficial/contrato vigente prevalece sobre ejemplos internos.

### 3.5. Pruebas E2E

Escenarios mínimos:

1. Compra → recepción → lote → precio → venta → CPE → ERP.
2. Venta bajo receta → validación/dispensación QF → POS.
3. Producto controlado → receta especial → movimiento → conciliación.
4. Devolución → nota de crédito → producto no vendible → disposición.
5. Recall → bloqueo omnicanal → inmovilización → conciliación.
6. Farmacovigilancia sin venta de la cadena.
7. Cierre de turno → posting ERP idempotente.

## 4. Suites críticas

| Suite | Riesgo | Ejemplos |
|---|---|---|
| `TST-INV-*` | Integridad de existencias | doble reserva, stock negativo, bloqueo, lote |
| `TST-DSP-*` | Acto farmacéutico | técnico intenta dispensar, receta inválida |
| `TST-CTL-*` | Fiscalizados | vigencia especial, adulteración, conciliación |
| `TST-POS-*` | Retail/fiscal | doble venta, CPE duplicado, cierre |
| `TST-DEV-*` | Devolución | NC no reingresa stock |
| `TST-RCL-*` | Seguridad sanitaria | lote bloqueado vendido por otro canal |
| `TST-FVG-*` | Privacidad | reporte sin ticket, acceso indebido |
| `TST-ERP-*` | Integración financiera | posting duplicado, reversa |
| `TST-SEC-*` | Autorización | rol/ámbito/competencia |

## 5. Datos de prueba

Se utilizarán datos sintéticos. No deberán emplearse recetas, pacientes, credenciales ni eventos reales de farmacovigilancia en ambientes de desarrollo/CI salvo un proceso formal de anonimización y autorización.

## 6. Criterio de salida

Para un flujo crítico no será suficiente aprobar el escenario positivo. Deben pasar sus CA de validación, competencia/seguridad, auditoría, concurrencia/idempotencia y recuperación cuando resulten aplicables.

## 7. Trazabilidad

- Fuente de aceptación: `docs/03-requerimientos/08-criterios-aceptacion.md`.
- Casos de uso: `docs/03-requerimientos/05-casos-uso.md`.
- Matriz: `docs/03-requerimientos/07-matriz-trazabilidad.md`.

La nomenclatura futura de pruebas conservará referencia al RF o CU, por ejemplo `TST-DSP-RF-DSP-016-001`.
