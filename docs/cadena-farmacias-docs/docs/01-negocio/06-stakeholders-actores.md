# DOC-FAR-006 — Stakeholders y Actores

## 1. Objetivo

Identificar quién participa o es afectado por la plataforma. Esta lista mezcla actores `NORM`, actores observados en el negocio retail (`MKT`) y roles internos `POR_VALIDAR`.

## 2. Stakeholders externos

| Stakeholder | Tipo | Interés |
|---|---|---|
| DIGEMID / Autoridad sanitaria competente | NORM | cumplimiento sanitario, establecimientos, productos, fiscalización |
| SUNAT | NORM | comprobantes, tributos y obligaciones fiscales |
| Colegio / registro profesional aplicable | NORM | habilitación/registro profesional según proceso |
| Proveedores / laboratorios / droguerías | MKT/FUNC | abastecimiento, documentos, lotes, condiciones comerciales |
| Clientes / pacientes | NORM/FUNC | compra, dispensación segura, privacidad, comprobantes |
| Bancos / adquirentes / PSP | MKT | pagos y conciliación |
| Operadores de delivery | MKT | fulfillment del pedido si se terceriza |
| Plataformas de e-commerce / marketplaces | POR_VALIDAR | canal digital |

## 3. Actores internos regulatorios

### 3.1 Director Técnico / Químico Farmacéutico

`NORM`

Responsabilidades del sistema a evaluar:

- identificación del profesional;
- establecimiento y horario asociado;
- presencia/cobertura según normativa aplicable;
- validaciones de dispensación que requieran intervención profesional;
- control/supervisión del personal;
- acciones sobre productos controlados cuando corresponda;
- trazabilidad de intervenciones.

### 3.2 Químico Farmacéutico Asistente

`NORM`

Puede ejecutar funciones profesionales dentro del ámbito permitido y bajo la organización establecida por el establecimiento.

### 3.3 Técnico en Farmacia

`NORM`

El D.S. N.° 015-2025-SA modificó el artículo 43 del Reglamento de Establecimientos Farmacéuticos y establece requisitos para el personal técnico, además de restricciones respecto de actos correspondientes a dispensación de productos de venta bajo receta y ofrecimiento de alternativas al medicamento prescrito.

Por ello, **el sistema no debe tratar a “cajero”, “técnico” y “QF” como usuarios equivalentes**.

## 4. Actores operativos propuestos

Estos roles son `MKT/FUNC/POR_VALIDAR` hasta entrevistas con la cadena.

- Administrador de cadena.
- Administrador de empresa.
- Jefe de tienda.
- Cajero.
- Vendedor / atención retail.
- Responsable de almacén de tienda.
- Jefe de centro de distribución.
- Operario de recepción.
- Picker / despachador.
- Comprador.
- Analista de abastecimiento.
- Gestor de precios/promociones.
- Contabilidad.
- Tesorería.
- Cuentas por pagar.
- Auditor interno.
- Servicio al cliente.
- E-commerce / fulfillment.
- Analista BI.
- Soporte TI.
- Administrador IAM.

## 5. Principio de autorización

La autorización futura deberá considerar como mínimo:

```text
Usuario
  +
Rol
  +
Empresa
  +
Establecimiento / almacén
  +
Caja / terminal cuando aplique
  +
Competencia profesional cuando aplique
  +
Acción
  ↓
PERMITIR / DENEGAR
```

Ejemplos:

- Un cajero puede cobrar una venta, pero no necesariamente validar una receta.
- Un técnico no debe recibir permisos de QF solo porque pertenezca a la misma tienda.
- Un comprador corporativo puede crear órdenes para varios almacenes sin tener acceso a caja POS.
- Un administrador TI no debe poder ejecutar dispensación farmacéutica por su rol técnico.
