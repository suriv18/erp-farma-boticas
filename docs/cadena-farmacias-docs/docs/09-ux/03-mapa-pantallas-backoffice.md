# UX-FAR-003 — Mapa de Pantallas Backoffice

## Organización

- Empresas
- Establecimientos
- Autorizaciones/vigencias
- Asignaciones profesionales

## Catálogo

- Productos regulados
- SKUs/presentaciones
- Condición de venta
- códigos de barra
- productos bloqueados/prohibidos

## Compras

- Solicitudes
- Aprobaciones
- Órdenes de compra
- Recepciones
- Diferencias / conciliación

## Inventario

- Stock por local/lote
- movimientos
- lotes por vencer
- bloqueados/recalled
- transferencias
- conteos/ajustes

## Retail

- ventas
- devoluciones
- cajas/turnos
- excepciones de precio

## Cumplimiento

- controlados
- recall
- farmacovigilancia
- CPE
- reporte de precios

## Dashboards

Los dashboards deben navegar al detalle con filtros preservados y respetar el scope del usuario.

## Catálogo de surtido diverso

Mostrar por separado rubro, régimen sanitario, evidencia, variantes y admisibilidad por local. Mostrar la política de lote/caducidad y por qué un SKU está bloqueado. El responsable revisa clasificación y evidencia antes de publicar; la UI no habilita capacidades aún no implementadas en inventario.

## Detalle de revisión definido

Separar borrador, verificación de evidencia, publicación de política y revisión de admisibilidad. Mostrar autor, fecha, versión, fuente y motivo; la NSO se lee desde expediente. Ante conflicto de versión, recargar y revisar diferencias sin sobrescribir. Series se capturan como unidades, variantes con controles de atributos. Contrato: [API-FAR-008](../07-api/08-contrato-surtido.md).
