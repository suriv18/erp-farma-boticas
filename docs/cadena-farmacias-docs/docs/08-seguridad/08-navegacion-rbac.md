# SEC-FAR-008 — Navegación dinámica y RBAC

## Decisión

El menú no forma parte del mecanismo de autorización. La seguridad se aplica en el backend mediante identidad validada + permisos + ámbito + reglas contextuales.

```text
Sesión/identidad
  + Rol
  + Permisos
  + Tenant/empresa/establecimiento/almacén/terminal
  + regla profesional/regulatoria
        ↓
   autorización real
```

La navegación consulta ese resultado únicamente para construir una experiencia coherente.

## Controles

- no aceptar roles/permisos desde el navegador;
- no confiar en que una ruta oculta esté protegida;
- no exponer PK internas;
- auditoría de denegaciones relevantes;
- cachear navegación por contexto y versión de autorización;
- cambios de rol/ámbito deben invalidar o versionar caché;
- el icono se interpreta mediante allow-list del frontend.
