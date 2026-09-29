# Diseño: Prototipo Figma de catálogos maestros simples (Fase 1 del prototipado general del ERP)

Fecha: 2026-09-28
Estado: propuesto

## Contexto

El usuario pidió prototipar en Figma todo el proyecto ERP de cadena de farmacias, reusando el archivo de Figma existente (`dAGg5r6W2taj38y4MMH3xH`, que hoy solo contiene la página "Login – Botica"). Dado que el backend está muy por detrás del modelo de datos (solo 2 de ~16 módulos tienen API real: `security` y `catalogo`), la fuente de verdad para decidir qué prototipar es el **modelo de datos** (18 schemas, ~90 tablas en `docs/cadena-farmacias-docs/database/migrations/V001-V018`), no el estado de implementación del backend.

Dado el tamaño del proyecto completo, se acordó trabajar por fases, empezando por completar los catálogos maestros simples. Al revisar el frontend se encontró que 7 de los 8 catálogos que `CLAUDE.md` documenta como "faltantes" ya tienen ruta y página implementada en `frontend/apps/erp-web/src/features/catalogo/` usando el componente genérico `SupportCatalogPage` (listado + modal crear + modal editar, parametrizado por `fields`/`columns`). Solo `principio_activo` no tiene página propia todavía, aunque el backend ya expone su API (`/api/v1/catalogo/principios-activos`). Se decidió prototipar las 8 pantallas igualmente desde cero en Figma, sin restringirse a replicar el código actual — la decisión de si el código se actualiza para igualar el nuevo diseño queda para una fase de implementación posterior, fuera de este spec.

## Alcance

Diseñar en Figma, como páginas nuevas dentro del archivo existente (junto a "Login – Botica"), las pantallas de los siguientes 8 catálogos maestros simples (todos de `sch_catalogo`, todos catálogos globales de una sola tabla, sin jerarquía):

1. `rubro_comercial`
2. `condicion_venta`
3. `forma_farmaceutica`
4. `via_administracion`
5. `unidad_medida`
6. `clasificacion_controlada`
7. `tipo_documento_identidad`
8. `principio_activo`

**Fuera de alcance de este spec:**
- `sku_comercial`, `producto_regulado`, `categoria_producto`, `marca` (ya tienen pantalla propia o son de mayor complejidad — fases futuras).
- Cualquier otro módulo de negocio (ventas, inventario, compras, dispensación, etc.).
- Implementación de código (backend o frontend) — este spec cubre únicamente el prototipo visual en Figma.
- Actualizar `CLAUDE.md` u otra documentación del estado del proyecto (queda anotado aquí como hallazgo, se corrige aparte si el usuario lo pide).

## Estructura en Figma

**Componente maestro "Catálogo simple"**, construido una sola vez y reutilizado vía instancias/variantes para los 8 catálogos. Partes del componente:

1. **Header de página**: breadcrumb ("Catálogo / {Nombre del catálogo}"), título, descripción corta, botón primario "+ Nuevo {recurso}" alineado a la derecha.
2. **Barra de filtros**: campo de búsqueda por texto (placeholder "Código o denominación") + select de Estado (Todos/Activo/Inactivo).
3. **Tabla**: columnas específicas por catálogo (ver mapeo abajo) + columna "Estado" (badge Activo/Inactivo) + columna "Acciones" (ícono editar, ícono activar/desactivar).
4. **Estados de la tabla**: contenido con datos, vacío ("No se encontraron registros."), cargando (skeleton/spinner), error.
5. **Modal "Crear {recurso}"**: formulario con los campos específicos del catálogo, botón "Crear".
6. **Modal "Editar {recurso}"**: mismo formulario, precargado con datos de ejemplo, botón "Guardar".

**8 instancias**, una por catálogo, con columnas/campos/datos de ejemplo reales:

| Catálogo | Columnas de tabla (además de código/denominación/estado) | Campos de formulario propios |
|---|---|---|
| Rubro comercial | Es farmacéutico (sí/no), Orden | Código, denominación, es farmacéutico, orden |
| Condición de venta | Requiere receta, Requiere retención, Fuente | Código, denominación, requiere receta, requiere retención, fuente |
| Forma farmacéutica | Fuente | Código, denominación, fuente |
| Vía de administración | — | Código, denominación |
| Unidad de medida | Símbolo, Permite decimal | Código, símbolo, permite decimal |
| Clasificación controlada | Requiere receta especial, Retiene receta, Vigencia receta (días) | Código, denominación, requiere receta especial, retiene receta, vigencia receta (días) |
| Tipo de documento de identidad | Sigla, Longitud mín/máx | Código, sigla, denominación, longitud mínima, longitud máxima |
| Principio activo | Nombre normalizado, Fuente | Denominación, nombre normalizado, fuente |

Los datos de ejemplo usan valores reales y plausibles del dominio farmacéutico peruano (p. ej. formas farmacéuticas: Tableta, Jarabe, Cápsula; condiciones de venta: Venta libre, Con receta médica, Receta retenida).

## Paleta de color

Se reutiliza la paleta verde-teal ya definida para el login (tokens `success-*` del frontend, blanco para acentos), como línea visual consistente del sistema iniciada en esa pantalla. Ajustes puntuales de color para tablas/formularios (p. ej. tono de fondo de fila hover, borde de input) se resuelven durante el diseño en Figma siguiendo esa misma paleta, sin introducir un tercer color base.

## Verificación

Al ser un entregable de diseño (no código), la verificación es:
- Las 8 instancias del componente maestro existen como páginas/frames navegables en el archivo de Figma, cada una con su tabla, sus 2 modales y datos de ejemplo propios.
- Revisión visual del usuario en Figma antes de considerar la fase cerrada.
- No se ejecuta `pnpm check` ni tests — no hay cambios de código en este spec.

## Riesgos / decisiones abiertas

- Este spec no decide si, tras aprobarse el prototipo, el código de las 7 pantallas ya implementadas se actualiza para igualar el nuevo diseño, o si solo se implementa `principio_activo` (que no existe aún) con el nuevo diseño y las otras 7 quedan pendientes de una migración visual futura. Esa decisión se toma en una fase de implementación separada.
- `CLAUDE.md` describe el estado del frontend como "faltan 8 catálogos" cuando en realidad son 7 implementados + 1 pendiente (`principio_activo`); se deja como hallazgo, corrección de documentación fuera de este spec salvo que el usuario la pida explícitamente.
- El orden de las fases siguientes del prototipado general (SKU comercial/producto regulado, luego el resto de módulos de negocio) se define en una conversación de brainstorming aparte cuando se inicie cada fase.
