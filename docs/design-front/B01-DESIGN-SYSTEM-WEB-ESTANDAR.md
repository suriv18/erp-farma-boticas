# B01 — Estándar maestro de UI/UX y Design System para aplicaciones web

> **Tipo:** estándar de referencia reutilizable · **Versión:** 1.0 · **Fecha:** 2026-10-13  
> **Aplicación:** ERP, CRM, SaaS, sistemas administrativos, portales, plataformas comerciales y aplicaciones web transaccionales.  
> **Framework agnóstico:** React, Angular, Vue, Svelte o HTML/CSS.  
> **Estado:** propuesta base; debe validarse y adaptarse a cada producto.

## 0. Propósito y modo de uso

Definir un lenguaje de diseño consistente, accesible, adaptable, escalable y medible para **cualquier proyecto web**. Este documento no prescribe una biblioteca específica ni impone valores rígidos a todos los productos.

**Cómo adoptarlo en un proyecto nuevo:**
1. Copiar este documento al repositorio, por ejemplo `docs/standards/B01-DESIGN-SYSTEM-WEB-ESTANDAR.md`.
2. Completar la ficha de proyecto del §1 y decidir las excepciones justificadas.
3. Implementar tokens y componentes compartidos del §4–§8.
4. Elegir patrones de páginas según la necesidad (§9–§13), no aplicarlos indiscriminadamente.
5. Incorporar las verificaciones del §17 a la definición de terminado.
6. Registrar cambios de diseño y evaluar su impacto en pantallas existentes.

**Regla de interpretación:**
- **[NORMATIVO]**: exigencia de la fuente indicada, dentro de su alcance (p. ej., un criterio WCAG si el proyecto adopta WCAG 2.2 AA).
- **[INTERNO]**: estándar recomendado por este documento, configurable según el proyecto.
- **[CONDICIONAL]**: aplicar cuando corresponda al dispositivo, flujo, volumen de datos o criticidad.
- **[EJEMPLO]**: material ilustrativo; no obligación.

> **No confundir:** WCAG es un estándar de accesibilidad; Ant Design, Carbon, Material Design y GOV.UK son sistemas de diseño/guías; Nielsen Norman Group publica principios de usabilidad; Core Web Vitals son métricas. La adopción voluntaria de WCAG AA no implica automáticamente cumplimiento legal en todas las jurisdicciones.

## 1. Ficha de adopción por proyecto

Completar antes de iniciar la implementación:

| Parámetro | Valor del proyecto |
|---|---|
| Nombre / versión | `[proyecto] / [versión]` |
| Usuarios y tareas principales | `[roles, tareas, frecuencia]` |
| Dispositivos objetivo | `[desktop / tablet / móvil / POS / quiosco]` |
| Idiomas y formatos regionales | `[idiomas, zona horaria, moneda, fechas]` |
| Framework y biblioteca UI base | `[React/Angular/etc.; biblioteca elegida]` |
| Fuentes tipográficas y licencias | `[familia, origen, licencia]` |
| Identidad visual | `[marca, logotipo, semántica de colores]` |
| Objetivo de accesibilidad | `WCAG 2.2 AA (propuesto)` |
| Navegadores soportados | `[versiones y política de soporte]` |
| Tipo de navegación | `[sidebar / topbar / híbrido / sin shell]` |
| Densidad | `[cómoda / estándar / compacta]` |
| Volumen de datos esperado | `[registros, concurrencia, carga]` |
| Presupuesto de rendimiento | `[web vitals + métricas internas]` |
| Responsables de UX, UI y frontend | `[equipo]` |
| Excepciones aprobadas | `[enlace ADR / registro de decisión]` |

**Principio de portabilidad:** las medidas, colores y componentes se configuran mediante tokens. El documento no debe contener dependencias del dominio (ej. `sku`, `paciente`, `reclamo`) dentro de los componentes base.

## 2. Principios rectores [INTERNO]

1. **Claridad antes que decoración:** jerarquía visual entendible y terminología del usuario.
2. **Consistencia:** una misma acción se representa y comporta igual en todo el sistema.
3. **Accesibilidad desde el diseño:** no resolverla como corrección al final.
4. **Feedback del sistema:** carga, éxito, advertencia y fallo explícitos.
5. **Prevención y recuperación de errores:** mensajes específicos y conservación de datos válidos.
6. **Eficiencia:** minimizar clics, desplazamientos y tiempos muertos en tareas repetitivas.
7. **Diseño responsive por contenido:** reorganizar componentes cuando dejan de funcionar, no por dispositivo nominal.
8. **Reutilización controlada:** componentes compartidos, variantes definidas, sin CSS ad hoc por pantalla.
9. **Datos íntegros:** una tabla no debe «perder» registros por paginación/filtros mal implementados.
10. **Medición continua:** usabilidad, accesibilidad y rendimiento se verifican en producción y pruebas.

## 3. Arquitectura del Design System [INTERNO]

```text
Design System
├── Foundations
│   ├── Design tokens (primitivos y semánticos)
│   ├── Tipografía, color y espaciado
│   ├── Elevación, bordes, movimiento e iconografía
│   └── Grid, breakpoints y densidad
├── Primitives
│   ├── Button, Text, Icon, Surface, Stack, Grid
│   └── Field, Label, Input, Select, Checkbox, Radio
├── Components
│   ├── Dialog, Drawer, Tabs, Tooltip, Alert, Toast
│   ├── DataTable, Pagination, Search, FilterBar
│   ├── DatePicker, Combobox, FileUpload
│   └── EmptyState, Skeleton, ErrorState
├── Patterns
│   ├── ListPage, FormPage, DetailPage, Dashboard
│   ├── Wizard, TransactionWorkspace
│   └── Confirmation, UnsavedChanges
└── Governance
    ├── Documentación y ejemplos
    ├── Accesibilidad y pruebas
    ├── Versionado y cambios incompatibles
    └── Catálogo de decisiones / excepciones
```

**Separación de responsabilidades:** `design-system` no conoce endpoints, reglas de negocio ni entidades de un producto. Los módulos de negocio componen estos patrones.

## 4. Tokens de diseño [INTERNO]

### 4.1. Niveles

- **Primitivos:** escala de grises, espaciado y radios sin semántica (`--space-4`, `--gray-900`).
- **Semánticos:** intención de uso (`--color-text-primary`, `--color-danger`, `--surface-base`).
- **De componente:** ajustes justificados (`--button-height-md`, `--sidebar-width`).

**Preferencia:** componentes consumen tokens semánticos y no colores hexadecimales directos. Compatibilizar temas claro, oscuro y alto contraste cuando el producto lo requiera.

### 4.2. Escala de espacio

| Token | Valor base (px) | Uso típico |
|---|---:|---|
| `--space-1` | 4 | Separación mínima |
| `--space-2` | 8 | Elementos relacionados |
| `--space-3` | 12 | Controles compactos |
| `--space-4` | 16 | Campos y grupos |
| `--space-5` | 20 | Bloques pequeños |
| `--space-6` | 24 | Padding de página, secciones |
| `--space-8` | 32 | Grupos amplios |
| `--space-12` | 48 | Separaciones mayores |

La escala es **recomendación interna**, no requisito WCAG. Evitar valores arbitrarios sin documentar la necesidad.

### 4.3. Colores y estados

Usar roles semánticos: `primary`, `neutral`, `success`, `warning`, `danger`, `info`; además `surface`, `border`, `text`, `focus`. Un estado **nunca** debe comunicarse solo mediante color. La paleta de marca se configura por proyecto.

**Contraste WCAG 2.2, nivel AA (síntesis):**
- Texto regular al menos **4.5:1**; texto grande al menos **3:1**, con excepciones contempladas por WCAG (criterio 1.4.3).
- Componentes e indicadores gráficos necesarios para comprender o usar la UI: normalmente **3:1** frente a colores adyacentes (criterio 1.4.11, con excepciones).
- Validar estados hover, foco, disabled y modo oscuro; no asumir que un color «se ve bien» equivale a conformidad.

### 4.4. Tipografía

| Rol | Tamaño inicial | Peso sugerido |
|---|---:|---|
| Título página | 24–28 px | 600–700 |
| Título sección | 18–20 px | 600 |
| Subtítulo | 16 px | 600 |
| Texto principal | 14–16 px | 400 |
| Texto auxiliar | 12–14 px | 400–500 |
| Etiqueta de campo | 13–14 px | 500–600 |

Son puntos de partida, no límites de accesibilidad. Priorizar unidades relativas (`rem`), zoom, altura de línea legible (aprox. 1.4–1.6 en párrafos) y resistencia a ampliación de texto. No reducir cuerpos por debajo de lo legible para conseguir mayor densidad.

### 4.5. Bordes, elevación y movimiento

- Radios sugeridos: 4, 8 y 12 px; emplear pocos niveles.
- Sombras suaves para capas elevadas; no sustituir bordes necesarios con sombras tenues.
- Animaciones funcionales, cortas y discretas; respetar `prefers-reduced-motion`.
- Iconos con una familia consistente; proporcionar nombres accesibles cuando son interactivos.

### 4.6. CSS de arranque (personalizable)

```css
:root {
  --space-1: 0.25rem;
  --space-2: 0.5rem;
  --space-3: 0.75rem;
  --space-4: 1rem;
  --space-5: 1.25rem;
  --space-6: 1.5rem;
  --space-8: 2rem;
  --space-12: 3rem;

  --color-primary: #1769aa; /* REEMPLAZAR por color de marca y verificar contraste */
  --color-text-primary: #1f2937;
  --color-text-muted: #475569;
  --color-surface: #ffffff;
  --color-canvas: #f7f9fc;
  --color-border: #d5dee8;
  --color-focus: #155eef;
  --color-danger: #b42318;
  --color-success: #067647;

  --radius-sm: 0.25rem;
  --radius-md: 0.5rem;
  --radius-lg: 0.75rem;

  --sidebar-expanded: 14rem;
  --sidebar-collapsed: 4rem;
  --header-height: 4rem;
  --page-gutter: 1.5rem;
  --form-max-width: 75rem;
}

@media (max-width: 64rem) {
  :root { --page-gutter: 1rem; }
}

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    scroll-behavior: auto !important;
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

> Los hexadecimales anteriores son **valores de ejemplo**, no una paleta certificada. Validar contraste de cada combinación, especialmente texto en botones primarios.

## 5. Layout, navegación y espacio útil [INTERNO]

### 5.1. Shell para aplicaciones administrativas [CONDICIONAL]

No toda web necesita sidebar: una landing, portal público o tienda puede requerir navegación superior. En aplicaciones con módulos numerosos, usar estructura:

```text
┌────────────────┬────────────────────────────────────────────┐
│ Sidebar        │ Header / utilidades                        │
│ navegación     ├────────────────────────────────────────────┤
│                │ Breadcrumb (cuando aporta orientación)     │
│                │ Título y acciones principales              │
│                │                                            │
│                │ Contenido principal                        │
│                │                                            │
└────────────────┴────────────────────────────────────────────┘
```

| Parámetro | Valor inicial configurable |
|---|---|
| Sidebar expandido | 224–240 px |
| Sidebar colapsado | 64–72 px |
| Header | 56–64 px |
| Padding horizontal de escritorio | 24 px |
| Padding horizontal de pantallas menores | 16 px |
| Formulario largo | hasta 1100–1200 px cuando sea útil |
| Tabla / grid / dashboard | ancho disponible, sin max-width arbitrario |

**Importante:** la separación lateral es el padding del área principal; no agregar simultáneamente márgenes duplicados en `main`, `page`, `card` y `form`. Formularios pueden tener ancho controlado; tablas suelen necesitar amplitud. Usar `min-width: 0` en hijos flex/grid para evitar desbordamientos.

```css
.app-shell { display: grid; grid-template-columns: var(--sidebar-expanded) minmax(0, 1fr); min-height: 100dvh; }
.app-main { min-width: 0; background: var(--color-canvas); }
.page { min-width: 0; padding: var(--space-6) var(--page-gutter); }
.page--form { width: 100%; max-width: var(--form-max-width); margin-inline: 0 auto; }
.page--data { width: 100%; max-width: none; }
@media (max-width: 48rem) {
  .app-shell { display: block; }
  /* El sidebar pasa a drawer accesible controlado por estado. */
}
```

### 5.2. Responsive según comportamiento

Breakpoints **orientativos**, ajustables a componentes y contenido:

- `>= 1440px`: escritorio amplio; mantener líneas de lectura razonables.
- `1024–1439px`: escritorio habitual; reducir columnas donde falte espacio.
- `768–1023px`: tablet; navegación colapsable y formulario flexible.
- `< 768px`: móvil; drawer de navegación, controles apilados y acciones visibles.

Probar también a **320 CSS px equivalentes y zoom del 400 %**, donde el criterio WCAG de reflow aplique (1.4.10 y sus excepciones). No forzar desplazamiento horizontal de la página; ciertas tablas y contenidos bidimensionales pueden disponer de desplazamiento propio justificable.

### 5.3. Navegación

- Indicar módulo activo, jerarquía y ubicación.
- No depender solo de iconos sin nombre accesible.
- El sidebar colapsado debe preservar comprensión (tooltip/nombre accesible).
- Breadcrumbs solo si existen niveles reales de navegación.
- Navegación y foco previsibles al cambiar de ruta.
- Recuperar estado de filtros/listas al volver desde el detalle cuando mejore el flujo.

## 6. Componentes base: contrato de calidad

Cada componente documentará: propósito, variantes, tamaños, props/inputs, eventos, estados, reglas responsive, semántica accesible, uso correcto/incorrecto, pruebas y ejemplos.

| Componente | Estados mínimos | Reglas esenciales |
|---|---|---|
| Botón | default, hover, focus, loading, disabled | Jerarquía primaria/secundaria; acción comprensible; no duplicar clics |
| Input | vacío, valor, focus, error, disabled, readonly | Label visible; ayuda y error ligados semánticamente |
| Select / Combobox | cerrado, abierto, búsqueda, error | Teclado y ARIA APG según patrón; no implementar a medias |
| Checkbox / Radio | activo, seleccionado, foco, error | Grupo etiquetado; navegación predecible |
| Dialog | abierto, ocupado, cierre | Gestionar foco inicial, contención de foco y retorno al disparador |
| Drawer | abierto, cerrado | Modalidad, foco y escape coherentes |
| Toast / Alert | info, éxito, warning, error | Anunciar apropiadamente; no depender solo del toast para errores de campo |
| DataTable | loading, empty, error, data | Orden/filtros/paginación; encabezados semánticos; responsividad |
| Pagination | inicio, medio, final | Controles claros y accesibles; conservar búsqueda/filtro |
| FileUpload | vacío, progreso, error, completo | Restricciones explícitas; fallos recuperables; validación servidor |
| Date / Number | válido, inválido, vacío | Entrada localizada; formatos explícitos; no asumir idioma |

**Nota:** preferir HTML semántico nativo cuando resuelva el problema. Añadir ARIA solo cuando corresponda; ARIA mal aplicado puede empeorar la accesibilidad.

## 7. Formularios y validación [INTERNO]

1. Agrupar por objetivo del usuario, no por estructura de tablas de BD.
2. Utilizar una o dos columnas según el contenido y la anchura. Un campo largo puede ocupar todo el ancho.
3. Etiquetar campos, unidades, formatos y obligatoriedad; los placeholders no sustituyen labels.
4. Validar formato en `blur` cuando sea oportuno; las dependencias entre campos al cambiar; validar siempre antes de guardar y en backend.
5. Mantener los valores introducidos después de errores recuperables.
6. Mostrar mensaje específico junto al campo y resumen de errores cuando el formulario es largo.
7. Evitar validación agresiva mientras el usuario todavía escribe si genera ruido innecesario.
8. Confirmar pérdida de cambios no guardados al salir cuando exista riesgo real.
9. Restringir campos condicionales solo por reglas explícitas.
10. Al completar: feedback y siguiente paso predecible (continuar, ver detalle, regresar al listado).

**Ejemplo de mensaje:** «Ingresa un correo válido, por ejemplo nombre@empresa.com», en lugar de «Error de formato».

### Acciones fijas/sticky

Si hay footer sticky con «Cancelar / Guardar», reservar espacio inferior en el contenedor con scroll, prevenir cobertura de campos, respetar áreas seguras móviles y cuidar el orden de foco. No fijarlo automáticamente en todos los formularios.

## 8. Tablas, búsquedas y paginación [INTERNO + CONDICIONAL]

**Contrato de listado:**
- Búsqueda y filtros actúan sobre el universo correspondiente, no solo los registros cargados de una página.
- Ordenación determinística: incluir desempate estable (p. ej., ID) cuando se requiere navegación consistente.
- Paginación backend para conjuntos medianos/grandes; tamaños orientativos 10/25/50/100 según costo.
- Informar `total`, `hasNext` u otra navegación válida según la API; no inventar totales si el backend no los conoce.
- Al cambiar filtros, normalmente regresar a primera página.
- Preservar estado de filtros/sort en URL cuando sea útil y seguro.
- Exportación conforme a filtros y permisos; distinguir «exportar página» y «exportar todos los resultados».
- Considerar virtualización si renderizar muchas filas afecta la fluidez; no sustituye paginación de BD.
- Acciones de fila claramente identificadas; evitar truncar campos críticos sin acceso al valor completo.
- Manejar datos cambiantes: paginación por cursor/keyset cuando offset genera inconsistencia o costo elevado.
- Implementar estados: initial, loading, empty, no-results, error, success, partial.

**Contrato API de referencia [EJEMPLO]:**

```json
{
  "items": [],
  "page": 0,
  "size": 25,
  "totalElements": 1250,
  "totalPages": 50,
  "sort": ["name,asc", "id,asc"]
}
```

La forma definitiva puede ser por `cursor` o `page`, pero debe quedar documentada y probada. Nunca depender de una lista truncada silenciosamente.

## 9. Patrones de pantallas [INTERNO]

| Patrón | Estructura | Ejemplos posibles |
|---|---|---|
| `ListPage` | título, acciones, filtros, tabla, paginación | productos, pedidos, usuarios |
| `FormPage` | encabezado, grupos de campos, validación, acciones | crear/editar registro |
| `DetailPage` | identificación, estado, datos, relaciones, historial | detalle de orden |
| `DashboardPage` | métricas relevantes, alertas y accesos de tarea | resumen operativo |
| `WizardPage` | pasos dependientes, progreso, validación por etapa | proceso de alta complejo |
| `TransactionWorkspace` | búsqueda veloz, selección, cálculo, confirmación | caja / POS / atención |
| `SettingsPage` | secciones pequeñas y cambios controlados | permisos / preferencias |

**Regla:** los patrones comparten componentes, pero pueden tener layouts distintos. No imponer ancho fijo de formulario a un dashboard o una tabla extensa.

## 10. Estados, feedback y prevención de errores

| Situación | Tratamiento |
|---|---|
| Carga inicial | skeleton si preserva disposición; indicador cuando corresponda |
| Guardando | desactivar doble envío o implementar idempotencia; estado visible |
| Éxito | confirmación breve y actualización coherente de la UI |
| Error de campo | mensaje concreto, asociación accesible, conservar valor |
| Error de servidor | explicar sin filtrar datos sensibles; reintento cuando proceda |
| Sin registros | estado vacío con siguiente acción útil |
| Búsqueda sin resultados | mostrar criterios y opción de limpiar filtros |
| Sin permiso | explicar limitación; el backend siempre valida autorización |
| Operación destructiva | confirmación proporcionada al daño posible |
| Sesión vencida | advertir y preservar trabajo cuando sea viable y seguro |
| Conflicto de edición | indicar colisión; opción de recargar/conciliar según negocio |

No confiar únicamente en `toast`; errores persistentes deben permanecer en el contexto correspondiente.

## 11. Accesibilidad [NORMATIVO si se adopta WCAG 2.2 AA]

**Objetivo recomendado:** WCAG 2.2 AA (no declarar conformidad sin evaluación completa).

Checklist crítico:

- [ ] Contraste del texto (1.4.3) y contraste no textual relevante (1.4.11).
- [ ] Toda interacción disponible mediante teclado, sin trampas de foco (2.1.1, 2.1.2).
- [ ] Indicador de foco visible (2.4.7) y foco no oculto al mínimo exigido (2.4.11).
- [ ] Objetivos táctiles conforme a 2.5.8 nivel AA: al menos 24×24 CSS px **o excepciones de separación/otros casos**; tamaños de 40–44 px son **recomendación interna** para comodidad, no requisito universal WCAG AA.
- [ ] Estructura de encabezados, landmarks, labels y nombres accesibles.
- [ ] Instrucciones/errores comprensibles y relacionados con controles.
- [ ] Reflow y zoom aplicables sin pérdida de contenido/funcionalidad.
- [ ] Mensajes de estado expuestos a tecnologías asistivas (4.1.3).
- [ ] Motion reducido cuando corresponda; evitar parpadeos peligrosos.
- [ ] Flujos de autenticación compatibles con accesibilidad, incluyendo 3.3.8 cuando aplique.

**Pruebas:** teclado sin mouse, lector de pantalla representativo, zoom/reflow, contraste automático + revisión manual, formularios con errores, modales y tablas. Las herramientas automáticas no prueban por sí solas la conformidad.

## 12. Internacionalización y contenido

- Definir idioma de documento y cambios de idioma correctamente.
- Formatear fecha, hora, moneda, decimales y unidades según locale; persistir valores sin ambigüedad.
- Escribir microcopy claro y orientado a acciones (verbos: «Guardar», «Descartar», «Descargar»).
- No depender de longitudes fijas de texto; prever traducciones más largas.
- Documentar pluralización, mensajes de validación y formatos de importación/exportación.
- Evitar exponer códigos técnicos o stack traces a usuarios finales.

## 13. Responsive, dispositivos y densidad

- Determinar tamaños por **contenido real**, no solo por nombres de dispositivos.
- Definir densidad `comfortable`, `default` y `compact` solo si los usuarios se benefician; compact no elimina contraste/foco.
- Establecer ancho mínimo y scroll interno para tablas densas, no para toda la página.
- Mantener acciones principales descubribles en pantallas estrechas.
- En POS/quioscos, priorizar ritmo operativo y teclado/escáner; no reutilizar ciegamente layouts administrativos.

## 14. Rendimiento percibido y real

**Core Web Vitals (umbrales «good» de referencia de Google):**

| Métrica | Meta | Significado |
|---|---:|---|
| LCP | ≤ 2.5 s | Renderización del contenido principal |
| INP | ≤ 200 ms | Capacidad de respuesta |
| CLS | ≤ 0.1 | Estabilidad visual |

Se evalúan típicamente al percentil 75 con datos de usuarios reales cuando hay volumen suficiente. Estas métricas son referencias; añadir métricas propias para búsqueda, cambio de página y guardado.

Recomendaciones:
- Lazy-load por ruta / módulo; evitar paquetes enormes.
- Limitar renders y consultas redundantes; debounce de búsqueda cuando sea útil.
- Reservar espacio a imágenes y resultados asíncronos para evitar saltos.
- Cachear con política definida; invalidar tras mutaciones.
- Medir con datasets representativos, incluso cientos de miles de registros en backend.
- No mostrar éxito antes de confirmación efectiva del servidor.

## 15. Seguridad relacionada con UI/UX

- Autorización real del lado del servidor para cada operación; ocultar controles no equivale a seguridad.
- Manejo explícito de sesión expirada, reautenticación y errores 401/403.
- Prevención de doble envío; idempotencia en transacciones sensibles.
- Mensajes sin revelar datos privados, SQL, secretos o trazas.
- Sanitización/escape al renderizar contenido del usuario; evitar HTML no confiable.
- Subidas de archivos validadas tanto en cliente como servidor.
- Evaluar bajo OWASP ASVS y guías relevantes para la tecnología elegida.

## 16. Organización técnica sugerida (independiente del framework)

```text
src/
├── app/
│   ├── bootstrap/
│   ├── routes/
│   └── layouts/
├── design-system/
│   ├── tokens/
│   ├── primitives/
│   ├── components/
│   ├── patterns/
│   └── docs/
├── features/
│   └── [feature-name]/
│       ├── pages/
│       ├── components/
│       ├── application/
│       └── data-access/
├── shared/
│   ├── accessibility/
│   ├── hooks-or-services/
│   ├── i18n/
│   ├── utilities/
│   └── types/
└── styles/
```

**React:** componentes, hooks y librería UI elegida; **Angular:** standalone components, servicios/directivas y CDK; **Vue:** componentes/composables. Conservar las reglas independientemente del framework.

## 17. Quality Gates y definición de terminado

### 17.1. Checklist de una pantalla

- [ ] Sigue patrón apropiado (lista, formulario, detalle, etc.).
- [ ] Solo usa tokens y componentes aprobados (o excepción justificada).
- [ ] Encabezado, breadcrumbs, acciones y jerarquía consistentes.
- [ ] No desperdicia ancho útil ni genera scroll horizontal injustificado.
- [ ] Funciona en tamaños y zoom definidos por el proyecto.
- [ ] Teclado y foco funcionan; labels y estados están bien anunciados.
- [ ] Tiene estados loading, empty, error y success según proceda.
- [ ] Validaciones conservan datos y muestran recuperación.
- [ ] Operaciones sensibles están protegidas contra duplicación.
- [ ] Datos grandes: orden, filtros, paginación/exportación correctos.
- [ ] Localización, formatos y textos revisados.
- [ ] Pruebas unitarias/componentes/E2E según criticidad.
- [ ] Revisión de rendimiento y seguridad correspondiente.
- [ ] Documentación y capturas de comportamiento actualizadas.

### 17.2. Pruebas recomendadas por capa

| Capa | Qué verificar | Ejemplos de herramientas (opcionales) |
|---|---|---|
| Diseño | variantes, tokens, consistencia | Storybook, Figma |
| Accesibilidad automática | fallos detectables por reglas | axe-core, Lighthouse |
| Accesibilidad manual | foco, teclado, lector de pantalla | NVDA / VoiceOver |
| Componentes | estados, eventos, validación | Testing Library, Vitest/Jest |
| End-to-end | flujos de negocio y errores | Playwright, Cypress |
| Rendimiento | Web Vitals, interacción, bundles | Lighthouse, DevTools, RUM |
| Seguridad | verificación de controles | OWASP ASVS y pruebas de API |

### 17.3. Gobierno del estándar

- Mantener versionado semántico del Design System.
- Un cambio de componente debe incluir estado anterior, impacto y plan de migración.
- Registrar excepciones específicas de proyectos con justificación y vencimiento/revisión.
- Evitar ramas divergentes de componentes base; priorizar variantes parametrizadas.
- Auditar trimestralmente la biblioteca y las pantallas de mayor uso.

## 18. Plantilla de personalización por proyecto

Crear `docs/design-system/PROJECT-TOKENS.md` o equivalente:

```md
# Perfil de diseño: [nombre del proyecto]
- Base: B01 — Estándar maestro UI/UX v1.0
- Framework: [React / Angular / Vue / otro]
- Biblioteca de componentes: [una base principal]
- Fuente: [familia]
- Color primario: [hex; contraste validado]
- Color secundario: [hex; contraste validado]
- Navegación: [sidebar / topnav / híbrida]
- Sidebar: [ancho / comportamiento]
- Header: [alto]
- Página: [gutter / ancho]
- Formulario: [max-width por patrón]
- Densidad: [normal / compacta]
- Breakpoints: [definidos por comportamiento]
- Modo oscuro: [sí/no]
- Idiomas: [lista]
- Objetivo accesibilidad: [WCAG AA u otro]
- Excepciones: [lista y justificación]
```

### 18.1. Ejemplo: ERP de boticas (NO parte obligatoria del estándar)

- Aplicación interna con sidebar de 224 px, header 64 px, gutter 24 px.
- Formularios extensos hasta 1200 px; listados ocupan ancho disponible.
- Catálogo y existencias usan paginación backend con filtro/orden consistentes.
- Pantalla POS puede utilizar layout diferente para velocidad de cobro.
- Colores, dominios y terminología se documentan en archivo de perfil del ERP, **no** dentro del estándar maestro.

## 19. Referencias oficiales y verificables

Consultarlas en la versión vigente antes de declarar conformidad o hacer actualizaciones mayores:

1. W3C, **Web Content Accessibility Guidelines 2.2**: https://www.w3.org/TR/WCAG22/
2. W3C WAI, **ARIA Authoring Practices Guide**: https://www.w3.org/WAI/ARIA/apg/
3. W3C WAI, **Forms Tutorial**: https://www.w3.org/WAI/tutorials/forms/
4. Ant Design, **Design Values / Components**: https://ant.design/docs/spec/values/ y https://ant.design/components/overview/
5. IBM, **Carbon Design System**: https://carbondesignsystem.com/
6. Google, **Material Design 3**: https://m3.material.io/
7. GOV.UK, **Design System / Error Messages**: https://design-system.service.gov.uk/ y https://design-system.service.gov.uk/components/error-message/
8. Nielsen Norman Group, **10 Usability Heuristics**: https://www.nngroup.com/articles/ten-usability-heuristics/
9. Google web.dev, **Web Vitals**: https://web.dev/articles/vitals
10. OWASP, **Application Security Verification Standard**: https://owasp.org/www-project-application-security-verification-standard/

## 20. Registro de decisiones y próximas actividades

| Código | Entregable | Prioridad |
|---|---|---|
| B01 | Fundamentos y estándares maestros (este documento) | Alta |
| B02 | Tokens definitivos y temas de cada proyecto | Alta |
| B03 | Componentes base y catálogo accesible | Alta |
| B04 | Layouts y plantillas de páginas | Alta |
| B05 | Formularios, tablas y flujos transaccionales | Alta |
| B06 | Evaluación WCAG, teclado y responsive | Alta |
| B07 | Automatización de pruebas y presupuesto de rendimiento | Media-Alta |
| B08 | Gobierno, versionado y adopción en nuevos proyectos | Media |

---

**Regla final:** el estándar maestro permanece **agnóstico al dominio** y a la tecnología. Cada proyecto crea su perfil y sus excepciones; evita copiar y modificar indiscriminadamente el estándar sin control de versiones.
