# Roadmap Scrum para ERP de Cadena de Boticas

> **Estado: SUSTITUIDO.** Las estimaciones de 26 semanas, Sprint 6 y Sprint 12 no son compromisos
> vigentes. El único roadmap activo es
> [`docs/cadena-farmacias-docs/docs/12-roadmap/01-roadmap-por-fases.md`](../cadena-farmacias-docs/docs/12-roadmap/01-roadmap-por-fases.md),
> gobernado por capacidades, evidencia y gates.

## 1. Contexto del proyecto

El proyecto corresponde a un **ERP para una cadena de boticas en Perú**, orientado a operar desde:

- App web ERP / Backoffice.
- App web POS.
- App web ecommerce.
- App móvil para cliente.
- App móvil para inventario, supervisor o repartidor.

El sistema debe cubrir el core de negocio de boticas: venta de productos farmacéuticos, perfumería, cosméticos, prendas, cuidado personal y otras categorías. También debe considerar inventario por lotes y vencimiento, compras, proveedores, ventas, caja, facturación, CRM, CMR, seguridad RBAC, auditoría y operación multisucursal.

## 2. Stack tecnológico definido

### Backend

- Java 25.
- Spring Boot.
- Gradle multimódulo.
- PostgreSQL.
- Flyway.
- Configuración con `application.yml`.
- Clean Architecture.
- DDD.
- Ports and Adapters.
- CQRS.
- Result Pattern.
- Seguridad con JWT/OAuth2 según alcance final.

### Frontend

- React.
- Vite.
- TypeScript.
- React Router.
- TanStack Query o equivalente para consumo de APIs.
- Manejo de estado por dominio.
- Componentes reutilizables.
- Diseño responsive para web, POS y vista móvil.

## 3. Supuestos de planificación Scrum

- Duración sugerida por sprint: **2 semanas**.
- Modalidad: backend y frontend avanzan en paralelo.
- Sprint 0: preparación técnica y funcional.
- Total recomendado: **13 sprints por equipo**, considerando Sprint 0 + 12 sprints de construcción.
- Duración total estimada: **26 semanas**, aproximadamente 6 meses.
- Entregas incrementales cada sprint.
- MVP funcional esperado: al cierre del Sprint 6.
- Versión beta esperada: al cierre del Sprint 9.
- Versión candidata a producción: al cierre del Sprint 12.

## 4. Equipo Scrum sugerido

### Roles Scrum

| Rol | Responsabilidad |
|---|---|
| Product Owner | Priorizar backlog, validar reglas de negocio y aprobar entregables. |
| Scrum Master | Facilitar ceremonias, remover impedimentos y cuidar la metodología. |
| Backend Developers | Construir APIs, dominio, persistencia, seguridad e integraciones. |
| Frontend Developers | Construir interfaces web, POS, ecommerce y vistas móviles. |
| QA | Diseñar pruebas funcionales, regresión, integración y validación de criterios. |
| UX/UI | Diseñar flujos, prototipos, componentes y experiencia de usuario. |
| DBA / Arquitecto | Revisar modelo de datos, performance, seguridad, escalabilidad y arquitectura. |
| DevOps | CI/CD, ambientes, despliegue, monitoreo y configuración. |

## 5. Definición de listo — Definition of Ready

Una historia de usuario estará lista para entrar a sprint cuando tenga:

- Descripción funcional clara.
- Criterios de aceptación.
- Reglas de negocio identificadas.
- Dependencias conocidas.
- Diseño o wireframe, si corresponde.
- Contrato API definido o mock disponible.
- Tablas o entidades identificadas.
- Prioridad asignada por el Product Owner.

## 6. Definición de terminado — Definition of Done

Una historia se considera terminada cuando cumple:

- Código implementado y revisado.
- Pruebas unitarias mínimas.
- Pruebas de integración cuando aplique.
- Validación de reglas de negocio.
- Manejo de errores con Result Pattern.
- Auditoría aplicada si corresponde.
- Seguridad/RBAC validada.
- Migración Flyway revisada si aplica.
- Endpoint documentado con OpenAPI.
- Frontend integrado al backend o mock aprobado.
- QA funcional aprobado.
- Pull request aprobado.

# 7. Roadmap de Backend

## Resumen Backend

| Campo | Valor |
|---|---|
| Total recomendado | 13 sprints |
| Sprint 0 | Preparación técnica |
| Sprints funcionales | 12 |
| Duración por sprint | 2 semanas |
| Duración total | 26 semanas aprox. |
| Primer MVP | Sprint 6 |
| Beta | Sprint 9 |
| Release candidata | Sprint 12 |

## Sprint 0 — Preparación técnica y arquitectura base

**Objetivo:** preparar el proyecto backend para trabajar de forma ordenada, modular y escalable.

### Alcance

- Crear repositorio backend.
- Crear estructura Gradle multimódulo.
- Configurar Java 25.
- Configurar Spring Boot.
- Configurar PostgreSQL.
- Configurar Flyway.
- Crear `application.yml`, `application-dev.yml`, `application-prod.yml`.
- Crear módulos base según Clean Architecture.
- Crear paquetes base por dominio.
- Definir estándares de nombres.
- Definir estructura de errores con Result Pattern.
- Configurar logging.
- Configurar OpenAPI/Swagger.
- Configurar Docker Compose local.
- Crear pipeline inicial CI.

### Entregables

- Backend compilando correctamente.
- Estructura multimódulo creada.
- Conexión a PostgreSQL validada.
- Migración inicial de base de datos lista.
- Convención de arquitectura documentada.

---

## Sprint 1 — Seguridad, usuarios, RBAC y organización base

**Objetivo:** construir el núcleo de seguridad y estructura organizacional.

### Alcance

- Módulo `security`.
- Login.
- JWT.
- Usuarios.
- Roles.
- Permisos.
- Módulos del sistema.
- Relación usuario-rol-sucursal.
- Sesiones de usuario.
- Control de acceso por módulo, recurso y acción.
- Módulo `organizacion`.
- Empresa.
- Sucursales.
- Almacenes.
- Cajas.

### Entregables

- API de autenticación.
- API de usuarios.
- API de roles y permisos.
- API de sucursales, almacenes y cajas.
- RBAC funcional.
- Auditoría básica aplicada en tablas maestras.

---

## Sprint 2 — Catálogo comercial y catálogo farmacéutico

**Objetivo:** construir el catálogo general de productos y la extensión farmacéutica.

### Alcance

- Categorías.
- Subcategorías.
- Marcas.
- Laboratorios.
- Unidades de medida.
- Presentaciones.
- Productos/SKU.
- Código interno.
- Código de barras.
- Producto farmacéutico como extensión.
- Principio activo.
- Concentración.
- Forma farmacéutica.
- Condición de venta.
- Registro sanitario.
- Indicadores de control sanitario.

### Entregables

- API de productos.
- API de categorías.
- API de marcas/laboratorios.
- API de información farmacéutica.
- Búsqueda de productos por nombre, código, categoría y código de barras.

---

## Sprint 3 — Inventario base por sucursal, almacén, lote y vencimiento

**Objetivo:** controlar stock real por almacén, lote y fecha de vencimiento.

### Alcance

- Stock por almacén.
- Stock por lote.
- Fecha de vencimiento.
- Ubicación interna.
- Ajustes de inventario.
- Motivos de ajuste.
- Stock mínimo.
- Punto de reposición.
- Alerta de bajo stock.
- Alerta de vencimiento próximo.

### Entregables

- API de stock.
- API de lotes.
- API de ajustes.
- Consulta de disponibilidad por sucursal.
- Consulta de vencimientos.

---

## Sprint 4 — Compras, proveedores y recepción de mercadería

**Objetivo:** gestionar compras y aumentar stock desde recepción.

### Alcance

- Proveedores.
- Contactos de proveedor.
- Orden de compra.
- Detalle de orden de compra.
- Recepción de compra.
- Recepción parcial.
- Registro de lotes recibidos.
- Validación de vencimiento.
- Actualización de stock.
- Generación de movimiento de inventario.
- Cuenta por pagar inicial.

### Entregables

- API de proveedores.
- API de órdenes de compra.
- API de recepción de compra.
- Integración compra-inventario.
- Registro automático en kardex.

---

## Sprint 5 — Ventas POS, caja y pagos presenciales

**Objetivo:** implementar la venta presencial en botica.

### Alcance

- Apertura de caja.
- Cierre de caja.
- Venta POS.
- Detalle de venta.
- Validación de stock.
- Descuento por producto.
- Descuento por venta.
- Pago en efectivo.
- Pago con tarjeta o transferencia.
- Reserva/descuento de stock.
- Registro en kardex.
- Anulación controlada de venta.

### Entregables

- API de venta POS.
- API de caja.
- API de pagos.
- Flujo completo venta-pago-stock-kardex.
- Reglas de seguridad por rol: cajero, administrador y supervisor.

---

## Sprint 6 — Clientes, comprobantes y facturación electrónica base

**Objetivo:** completar el MVP de venta con cliente y comprobantes.

### Alcance

- Clientes.
- Tipo y número de documento.
- Boleta.
- Factura.
- Nota de crédito.
- Nota de débito.
- Comprobante electrónico.
- Estados de comprobante.
- Adaptador para SUNAT.
- Logs de envío.
- Reintentos.
- Representación impresa básica.

### Entregables

- API de clientes.
- API de comprobantes.
- Adaptador inicial de facturación.
- MVP funcional: catálogo + inventario + compras + venta POS + comprobante.

---

## Sprint 7 — Inventario avanzado, transferencias y reposición

**Objetivo:** mejorar la operación de inventario multisucursal.

### Alcance

- Transferencia entre almacenes.
- Solicitud de transferencia.
- Despacho.
- Recepción.
- Kardex avanzado.
- Sugerencia de reposición.
- Reposición por stock mínimo.
- Reposición por rotación.
- Reporte de productos inmovilizados.
- Reporte de próximos a vencer.

### Entregables

- API de transferencias.
- API de reposición.
- Consulta de kardex.
- Reportes operativos de inventario.

---

## Sprint 8 — Ecommerce, pedidos digitales y app móvil cliente

**Objetivo:** habilitar ventas por canal digital.

### Alcance

- Cuenta digital de cliente.
- Carrito de compras.
- Pedido digital.
- Reserva de stock omnicanal.
- Dirección de entrega.
- Validación de cobertura.
- Pedido para recojo en tienda.
- Pedido para delivery.
- Receta digital como adjunto.
- Estados de pedido.

### Entregables

- API de ecommerce.
- API de carrito.
- API de pedidos digitales.
- API de reservas de stock.
- API de recetas digitales.

---

## Sprint 9 — CRM, fidelización, campañas y cupones

**Objetivo:** gestionar relación comercial con clientes.

### Alcance

- Segmentos de cliente.
- Programa de puntos.
- Campañas.
- Cupones.
- Beneficios.
- Historial de interacciones.
- Reclamos.
- Encuestas de satisfacción.
- Preferencias del cliente.

### Entregables

- API CRM.
- API de campañas.
- API de cupones.
- API de puntos.
- API de reclamos.
- Versión beta funcional.

---

## Sprint 10 — RR. HH., trabajadores, turnos y control operativo

**Objetivo:** controlar trabajadores, asignaciones y operación por sucursal.

### Alcance

- Trabajadores.
- Cargos.
- Contratos.
- Trabajador por sucursal.
- Relación trabajador-usuario.
- Turnos laborales.
- Programación de turnos.
- Asistencia.
- Supervisores.
- Control de vendedores y cajeros.

### Entregables

- API de trabajadores.
- API de contratos.
- API de turnos.
- API de asistencia.
- Integración con seguridad y ventas.

---

## Sprint 11 — Finanzas, cuentas por cobrar, pagar y reportes administrativos

**Objetivo:** consolidar gestión financiera básica del ERP.

### Alcance

- Cuentas por cobrar.
- Cuentas por pagar.
- Bancos.
- Movimientos bancarios.
- Caja administrativa.
- Asientos contables simples.
- Conciliación básica.
- Reporte de ventas.
- Reporte de compras.
- Reporte de caja.
- Reporte de margen.

### Entregables

- API de finanzas.
- API de reportes administrativos.
- Integración ventas-finanzas.
- Integración compras-finanzas.

---

## Sprint 12 — Hardening, observabilidad, rendimiento y cierre de release

**Objetivo:** estabilizar el backend para producción.

### Alcance

- Pruebas de seguridad.
- Pruebas de rendimiento.
- Optimización de consultas.
- Índices adicionales.
- Revisión de transacciones.
- Control de concurrencia en stock.
- Logs estructurados.
- Métricas.
- Health checks.
- Documentación OpenAPI final.
- Corrección de bugs críticos.
- Scripts de despliegue.

### Entregables

- Release candidata backend.
- APIs documentadas.
- Pruebas de regresión aprobadas.
- Checklist técnico de producción.

# 8. Roadmap de Frontend

## Resumen Frontend

| Campo | Valor |
|---|---|
| Total recomendado | 13 sprints |
| Sprint 0 | Preparación UX/UI y arquitectura React |
| Sprints funcionales | 12 |
| Duración por sprint | 2 semanas |
| Duración total | 26 semanas aprox. |
| Primer MVP | Sprint 6 |
| Beta | Sprint 9 |
| Release candidata | Sprint 12 |

## Sprint 0 — Preparación UX/UI y arquitectura React

**Objetivo:** preparar la base del frontend para web ERP, POS, ecommerce y móvil.

### Alcance

- Crear repositorio frontend.
- Configurar React + Vite + TypeScript.
- Configurar rutas.
- Configurar estructura por dominios.
- Configurar cliente HTTP.
- Configurar manejo de sesión.
- Configurar manejo de errores.
- Configurar layout principal.
- Definir design system.
- Crear componentes base.
- Crear prototipo de navegación.
- Crear mocks de APIs iniciales.

### Entregables

- Proyecto React compilando.
- Layout base.
- Componentes base.
- Guía visual inicial.
- Estructura lista para ERP, POS y ecommerce.

---

## Sprint 1 — Login, seguridad, RBAC y layout administrativo

**Objetivo:** permitir acceso seguro y navegación por permisos.

### Alcance

- Pantalla de login.
- Manejo de token.
- Recuperación básica de sesión.
- Menú dinámico por permisos.
- Layout ERP.
- Layout POS.
- Administración de usuarios.
- Administración de roles.
- Administración de permisos.
- Pantallas de sucursales, almacenes y cajas.

### Entregables

- Login funcional.
- Navegación protegida.
- Menú por rol.
- CRUD visual de usuarios, roles y organización.

---

## Sprint 2 — Catálogo de productos

**Objetivo:** administrar productos de todas las categorías.

### Alcance

- Lista de productos.
- Búsqueda avanzada.
- Filtros por categoría, marca y estado.
- Formulario de producto.
- Formulario de datos farmacéuticos.
- Gestión de categorías.
- Gestión de marcas.
- Gestión de laboratorios.
- Vista de detalle de producto.

### Entregables

- Módulo de catálogo funcional.
- Formularios validados.
- Interfaz para productos farmacéuticos y no farmacéuticos.

---

## Sprint 3 — Inventario base

**Objetivo:** visualizar y controlar stock por sucursal, almacén, lote y vencimiento.

### Alcance

- Consulta de stock.
- Stock por sucursal.
- Stock por almacén.
- Stock por lote.
- Vencimientos.
- Alertas de bajo stock.
- Ajustes de inventario.
- Historial básico de movimientos.

### Entregables

- Pantalla de stock.
- Pantalla de lotes.
- Pantalla de ajustes.
- Alertas visibles para usuario operativo.

---

## Sprint 4 — Compras y recepción

**Objetivo:** permitir al usuario gestionar proveedores, compras y recepción de mercadería.

### Alcance

- Mantenimiento de proveedores.
- Orden de compra.
- Detalle de compra.
- Recepción total y parcial.
- Registro de lote y vencimiento.
- Vista de compras pendientes.
- Vista de historial de compras.

### Entregables

- Módulo visual de compras.
- Flujo compra-recepción-stock integrado.

---

## Sprint 5 — POS y caja

**Objetivo:** construir el flujo principal de venta presencial.

### Alcance

- Layout POS optimizado.
- Búsqueda rápida por producto y código de barras.
- Carrito POS.
- Cálculo de descuentos.
- Selección de cliente.
- Métodos de pago.
- Apertura de caja.
- Cierre de caja.
- Resumen de venta.
- Impresión o vista de comprobante.

### Entregables

- POS funcional.
- Venta presencial integrada.
- Flujo de caja básico.

---

## Sprint 6 — Clientes y comprobantes

**Objetivo:** completar MVP visual de ventas con comprobantes.

### Alcance

- Administración de clientes.
- Selección de tipo de comprobante.
- Boleta.
- Factura.
- Nota de crédito.
- Nota de débito.
- Estado de comprobantes.
- Vista de errores de envío.
- Reintentos.
- Representación impresa.

### Entregables

- MVP frontend completo.
- Venta POS con comprobante.
- Consulta de comprobantes.

---

## Sprint 7 — Inventario avanzado y transferencias

**Objetivo:** mejorar operación multisucursal desde interfaz.

### Alcance

- Solicitud de transferencia.
- Despacho de transferencia.
- Recepción de transferencia.
- Kardex avanzado.
- Sugerencias de reposición.
- Reporte de productos próximos a vencer.
- Reporte de bajo stock.

### Entregables

- Módulo de transferencias.
- Vistas de reposición.
- Kardex visual.

---

## Sprint 8 — Ecommerce y app móvil cliente

**Objetivo:** habilitar experiencia de compra digital.

### Alcance

- Home ecommerce.
- Listado de productos para cliente.
- Búsqueda pública.
- Detalle de producto.
- Carrito digital.
- Checkout.
- Dirección de entrega.
- Recojo en tienda.
- Delivery.
- Subida de receta digital.
- Estado del pedido.

### Entregables

- Flujo ecommerce funcional.
- Base responsive para app móvil o PWA.
- Carrito y pedido digital integrados.

---

## Sprint 9 — CRM, fidelización y campañas

**Objetivo:** habilitar gestión comercial y fidelización.

### Alcance

- Segmentos de cliente.
- Programa de puntos.
- Campañas.
- Cupones.
- Beneficios.
- Reclamos.
- Encuestas.
- Preferencias del cliente.
- Panel CRM.

### Entregables

- Módulo CRM visual.
- Panel de fidelización.
- Versión beta frontend.

---

## Sprint 10 — RR. HH., trabajadores, turnos y asistencia

**Objetivo:** administrar trabajadores y operación por sucursal.

### Alcance

- Lista de trabajadores.
- Ficha de trabajador.
- Contratos.
- Asignación a sucursal.
- Cargos.
- Turnos.
- Programación de turnos.
- Asistencia.
- Relación trabajador-usuario.

### Entregables

- Módulo RR. HH. funcional.
- Control básico de trabajadores y turnos.

---

## Sprint 11 — Finanzas y reportes

**Objetivo:** brindar visibilidad administrativa y financiera.

### Alcance

- Cuentas por cobrar.
- Cuentas por pagar.
- Bancos.
- Caja administrativa.
- Reporte de ventas.
- Reporte de compras.
- Reporte de stock.
- Reporte de margen.
- Dashboard gerencial.

### Entregables

- Módulo financiero visual.
- Dashboard de gestión.
- Reportes básicos exportables.

---

## Sprint 12 — QA, accesibilidad, performance y release

**Objetivo:** estabilizar frontend para producción.

### Alcance

- Corrección de bugs.
- Optimización de carga.
- Revisión responsive.
- Validación de formularios.
- Pruebas e2e principales.
- Pruebas de regresión.
- Revisión de accesibilidad básica.
- Manejo uniforme de errores.
- Revisión de permisos en pantalla.
- Preparación de build productivo.

### Entregables

- Release candidata frontend.
- Checklist funcional aprobado.
- Frontend integrado con backend final.

# 9. Roadmap integrado Backend + Frontend

| Sprint | Backend | Frontend | Meta integrada |
|---|---|---|---|
| Sprint 0 | Arquitectura, Gradle, BD, Flyway, Docker | React, Vite, layout, design system | Base técnica lista |
| Sprint 1 | Seguridad, RBAC, organización | Login, menú por permisos, usuarios | Acceso seguro y administración base |
| Sprint 2 | Catálogo y farmacia | Catálogo visual | Productos administrables |
| Sprint 3 | Inventario por lote/vencimiento | Stock, lotes y ajustes | Stock controlado |
| Sprint 4 | Compras y recepción | Proveedores, compras y recepción | Compras aumentan inventario |
| Sprint 5 | Ventas POS, caja y pagos | POS y caja | Venta presencial funcional |
| Sprint 6 | Clientes y comprobantes | Clientes y comprobantes | MVP operativo |
| Sprint 7 | Transferencias y reposición | Transferencias y kardex | Operación multisucursal |
| Sprint 8 | Ecommerce y pedidos digitales | Ecommerce y móvil cliente | Venta digital habilitada |
| Sprint 9 | CRM, puntos y campañas | CRM y fidelización | Beta comercial |
| Sprint 10 | RR. HH. y trabajadores | Trabajadores y turnos | Control operativo interno |
| Sprint 11 | Finanzas y reportes | Reportes y dashboard | Gestión administrativa |
| Sprint 12 | Hardening y performance | QA, performance y release | Release candidata |

# 10. Hitos del proyecto

## Hito 1 — Base técnica lista

**Fin del Sprint 0**

- Arquitectura creada.
- Repositorios listos.
- Base de datos conectada.
- Frontend base funcionando.

## Hito 2 — Administración base

**Fin del Sprint 2**

- Seguridad.
- Organización.
- Catálogo.
- Productos.

## Hito 3 — Inventario y compras

**Fin del Sprint 4**

- Stock.
- Lotes.
- Vencimientos.
- Compras.
- Recepción.

## Hito 4 — MVP operativo

**Fin del Sprint 6**

- Venta POS.
- Caja.
- Clientes.
- Comprobantes.
- Inventario actualizado.

## Hito 5 — Operación multisucursal y digital

**Fin del Sprint 8**

- Transferencias.
- Reposición.
- Ecommerce.
- Pedidos digitales.
- Delivery inicial.

## Hito 6 — Beta comercial

**Fin del Sprint 9**

- CRM.
- Fidelización.
- Cupones.
- Campañas.

## Hito 7 — ERP ampliado

**Fin del Sprint 11**

- RR. HH.
- Finanzas.
- Reportes.
- Dashboard.

## Hito 8 — Release candidata

**Fin del Sprint 12**

- Seguridad validada.
- Performance revisada.
- QA aprobado.
- Documentación técnica lista.

# 11. Priorización recomendada del backlog

## Prioridad 1 — Core obligatorio

- Seguridad/RBAC.
- Organización/sucursales.
- Catálogo.
- Inventario por lote/vencimiento.
- Compras.
- Ventas POS.
- Caja.
- Clientes.
- Comprobantes.

## Prioridad 2 — Operación de cadena

- Transferencias.
- Reposición.
- Kardex avanzado.
- Reportes de inventario.
- Control multisucursal.

## Prioridad 3 — Canales digitales

- Ecommerce.
- Pedido digital.
- Delivery.
- App móvil cliente.
- Receta digital.
- Pagos digitales.

## Prioridad 4 — Gestión comercial

- CRM.
- CMR.
- Fidelización.
- Campañas.
- Cupones.
- Reclamos.

## Prioridad 5 — ERP administrativo

- RR. HH.
- Finanzas.
- Reportes gerenciales.
- Dashboard.

# 12. Riesgos principales y mitigación

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Modelo demasiado grande para el primer release | Alto | Construir primero MVP hasta Sprint 6. |
| Integración SUNAT compleja | Alto | Trabajar con adaptador aislado y mocks desde Sprint 6. |
| Control de stock concurrente | Alto | Usar transacciones, bloqueo optimista/pesimista según caso y pruebas de concurrencia. |
| Exceso de módulos en paralelo | Medio | Respetar prioridades y no iniciar CRM antes del core. |
| Frontend bloqueado por APIs | Medio | Definir contratos OpenAPI y mocks por sprint. |
| Cambios frecuentes de reglas de negocio | Medio | Refinamiento semanal con Product Owner. |
| Seguridad RBAC incompleta | Alto | Validar permisos desde backend y frontend. |
| Performance en búsquedas POS | Alto | Índices, paginación, búsqueda por código de barras y caché controlada. |

# 13. Recomendación final

Para cumplir las metas con Scrum, se recomienda trabajar con **13 sprints por backend y 13 sprints por frontend**, ejecutados en paralelo.

La primera meta fuerte debe ser llegar al **MVP en el Sprint 6**, cubriendo:

- Login y RBAC.
- Organización.
- Catálogo.
- Inventario.
- Compras.
- Venta POS.
- Caja.
- Clientes.
- Comprobantes.

Después del MVP, el proyecto puede avanzar hacia operación de cadena, ecommerce, app móvil, CRM, CMR, RR. HH., finanzas y reportes.
