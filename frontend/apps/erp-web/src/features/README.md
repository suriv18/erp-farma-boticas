# Features de ERP Web

Cada carpeta de este directorio es un módulo funcional local de `erp-web`, no un workspace
pnpm independiente. Esta decisión conserva límites claros sin multiplicar manifiestos,
configuraciones ni builds.

## Módulos ensamblados

- `auth`: acceso público y ciclo de sesión.
- `dashboard`: resumen operativo.
- `seguridad`: usuarios, roles y permisos.
- `organizacion`: empresas, sucursales, almacenes y cajas.
- `catalogo`: productos y clasificaciones comerciales.
- `inventario`: stock, lotes y vencimientos.
- `compras`: proveedores, órdenes y recepción.
- `ventas`: operaciones comerciales y comprobantes.
- `pos`: experiencia de venta rápida y caja.
- `caja`: aperturas, movimientos, arqueos y cierres.
- `clientes`: administración e historial comercial.

## Contrato de un módulo

```text
feature/
├── index.ts       # API pública del módulo
├── routes.tsx     # rutas y lazy loading
├── pages/         # composición de pantallas
├── components/    # UI exclusiva, cuando exista
├── api/           # queries y mutations, cuando existan
├── schemas/       # validaciones de entrada, cuando existan
└── model/         # tipos y mappers, cuando existan
```

Solo `index.ts`, `routes.tsx` y las páginas implementadas son obligatorios. Las demás
carpetas se crean cuando contienen código real.

El router de la aplicación importa exclusivamente desde el `index.ts` de cada módulo. Un
módulo no debe importar archivos internos de otro; cualquier integración se realiza desde
la composición de `app` o mediante la API pública del módulo colaborador.
