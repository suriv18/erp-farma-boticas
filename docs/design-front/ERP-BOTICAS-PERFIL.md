# Perfil de adopción B01 — ERP Boticas

Fecha: 2026-10-13. Base: B01 v1.0. Alcance: frontend web administrativo y POS.

## Decisiones del producto

- React, TypeScript, Tailwind y componentes compartidos en `@boticas/ui-web`; Radix para interacciones modales.
- Usuarios: personal de farmacia, caja, compras, inventario y administración. Desktop, tablet y móvil; los flujos POS conservan su distribución propia.
- Idioma español; moneda PEN y locale es-PE. Zona de referencia America/Lima. Las fechas sin hora se mantienen como fechas civiles.
- Tipografía: Inter si está disponible, con fuentes de sistema de respaldo; no se distribuye un archivo Inter en este cambio.
- Marca teal; temas claro y oscuro. Botones primarios con primary-700 y texto blanco; hover primary-800.
- Sidebar de escritorio: 224 px desde 1024 px. En pantallas menores, navegación mediante diálogo lateral con foco contenido y Escape.
- Header existente: 80 px. Se conserva para mantener el estilo del ERP; excepción a los valores iniciales orientativos de B01.
- Padding horizontal: 16 px bajo 640 px; 24 px desde 640 px. La geometría se define en el layout compartido.
- Formularios, listados y dashboards ocupan todo el ancho disponible. **No existe límite de 1200 px**, por decisión del producto. No se reintroducirá como criterio de cumplimiento.
- Densidad estándar; controles principales de 44 px. No se añaden variantes compactas sin necesidad operativa.
- Navegadores de prueba: Chromium desktop y WebKit con perfiles iPad/iPhone de Playwright. Esto no reemplaza pruebas en dispositivos físicos ni define por sí solo una política de soporte.

## Contratos de interacción

- Los diálogos comparten gestión de foco, Escape y retorno al disparador. El cierre externo no descarta formularios accidentalmente.
- SKU y producto regulado advierten al abandonar datos modificados, mantienen valores tras errores y muestran un resumen enlazado a los campos.
- Los filtros de SKU y productos regulados se guardan en la URL. El navegador permite recuperar la consulta al volver.
- Los selectores paginados de Catálogo recorren las páginas del servidor; no se presentan los primeros 100 como catálogo completo. Si falla una página, se ofrece reintento en los formularios.
- La carga completa de opciones es apropiada para catálogos acotados. Antes de usar volúmenes masivos, sustituirla por búsqueda remota paginada y medir con datos representativos.
- El movimiento respeta la preferencia de animación reducida. Errores de campos se relacionan semánticamente con sus controles.

## Verificación y límites

- Objetivo: WCAG 2.2 AA. No se declara conformidad completa.
- Ejecutar `pnpm exec playwright test e2e/accessibility.spec.ts e2e/catalogo.spec.ts e2e/layout.spec.ts` para accesibilidad automática, flujos y geometría en los tres proyectos configurados.
- axe comprueba las páginas de alta en claro/oscuro. Teclado y recuperación de cambios tienen pruebas adicionales. Quedan pendientes lector de pantalla, zoom 400 %, dispositivos físicos y auditoría completa del resto de pantallas.
- Ejecutar lint, typecheck, pruebas de componentes afectadas y build en cada cambio compartido.
- Los Web Vitals se observan localmente mediante eventos `erp:web-vital` y marcas `erp:LCP`, `erp:INP`, `erp:CLS`, sin envío externo. Consultar marcas en DevTools Performance o con `performance.getEntriesByType('mark')`.
- Presupuestos propuestos: LCP ≤ 2500 ms, INP ≤ 200 ms, CLS ≤ 0,1. No hay aún evidencia de percentil 75 en producción; la instrumentación no equivale a cumplir esos presupuestos.
- Clientes sigue siendo una pantalla provisional. La revisión frontend no acredita controles de autorización backend ni pruebas de volumen.
- Responsables nominales, volumen operativo y política de soporte de versiones: pendientes de asignación por el equipo del producto. Revisar este perfil cuando cambien esos acuerdos o los componentes compartidos.
