-- Categorias reales verificadas el 2026-10-14 en el menu comercial de Boticas Peru.
-- Fuente de todos los nombres y relaciones padre/hijo:
-- https://www.boticasperu.pe/cuidado_personal/corporal
-- Se normalizan tildes en Botiquin e Higiene Intima; codigos internos propios.
-- Descripciones NULL: la fuente no publica descripciones de estas categorias.
-- Seleccion de categorias permanentes, sin marcas ni promociones. No es taxonomia regulatoria.
-- Ejecutar con psql -v ON_ERROR_STOP=1. Solo inserta; nunca actualiza registros existentes.
-- Destino explicitamente acotado al tenant FARMALAB.
\set ON_ERROR_STOP on
SET client_encoding = 'UTF8';
BEGIN;
SET LOCAL lock_timeout = '5s';
LOCK TABLE sch_catalogo.categoria_producto IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE categorias_carga (
    codigo text PRIMARY KEY, padre text, nombre text NOT NULL,
    descripcion text, orden integer NOT NULL
) ON COMMIT DROP;

INSERT INTO categorias_carga VALUES
('MED', NULL, 'Botica en Casa', NULL, 10),
('CPE', NULL, 'Cuidado Personal', NULL, 20),
('DER', NULL, 'Dermocosmética', NULL, 30),
('NUT', NULL, 'Nutrición', NULL, 40),
('BEB', NULL, 'Mamá y Bebé', NULL, 50),
('AUX', NULL, 'Botiquín', NULL, 60),
('DIS', NULL, 'Dispositivos Médicos', NULL, 70),
('ADU', NULL, 'Adulto Mayor', NULL, 80),
('SEX', NULL, 'Bienestar Sexual', NULL, 90),
('MED-ANALGESICOS', 'MED', 'Analgésicos', NULL, 10),
('MED-ANTIBIOTICOS', 'MED', 'Antibióticos', NULL, 20),
('MED-ANTIFUNGICOS', 'MED', 'Antifúngicos', NULL, 30),
('MED-ANTIHISTAMINICOS', 'MED', 'Antihistamínicos', NULL, 40),
('MED-ANTIINFLAMATORIOS', 'MED', 'Antiinflamatorios', NULL, 50),
('MED-GASTROINTESTINAL', 'MED', 'Gastrointestinal', NULL, 60),
('CPE-BUCAL', 'CPE', 'Bucal', NULL, 10),
('CPE-CABELLO', 'CPE', 'Cabello', NULL, 20),
('CPE-CORPORAL', 'CPE', 'Corporal', NULL, 30),
('CPE-INTIMA', 'CPE', 'Higiene Íntima', NULL, 40),
('CPE-JABONES', 'CPE', 'Jabones de Tocador', NULL, 50),
('DER-FACIALES', 'DER', 'Faciales', NULL, 10),
('DER-CORPORALES', 'DER', 'Corporales', NULL, 20),
('DER-SOLARES', 'DER', 'Solares', NULL, 30),
('NUT-VITAMINAS', 'NUT', 'Vitaminas y Minerales', NULL, 10),
('NUT-SUPLEMENTOS', 'NUT', 'Complementos y Suplementos', NULL, 20),
('NUT-COLAGENO', 'NUT', 'Colágeno', NULL, 30),
('BEB-PANALES', 'BEB', 'Pañales', NULL, 10),
('BEB-ASEO', 'BEB', 'Aseo Infantil', NULL, 20),
('BEB-FORMULA', 'BEB', 'Fórmula Infantil', NULL, 30),
('BEB-LACTANCIA', 'BEB', 'Accesorios de Lactancia', NULL, 40),
('AUX-GASAS', 'AUX', 'Gasas', NULL, 10),
('AUX-ALCOHOL', 'AUX', 'Alcohol', NULL, 20),
('AUX-VENDAS', 'AUX', 'Vendas y Esparadrapo', NULL, 30),
('DIS-MEDICION', 'DIS', 'Equipo de Medición', NULL, 10),
('DIS-QUIRURGICOS', 'DIS', 'Dispositivos Quirúrgicos', NULL, 20),
('DIS-ORTOPEDIA', 'DIS', 'Ortopedia', NULL, 30),
('ADU-INCONTINENCIA', 'ADU', 'Incontinencia', NULL, 10),
('ADU-FORMULAS', 'ADU', 'Fórmulas Adultos', NULL, 20),
('SEX-PRESERVATIVOS', 'SEX', 'Preservativos', NULL, 10),
('SEX-LUBRICANTES', 'SEX', 'Gel y Lubricantes', NULL, 20);
CREATE TEMP TABLE categorias_insertadas (id bigint, codigo text, nombre text) ON COMMIT DROP;

DO $$
DECLARE
    destino bigint;
    fila record;
    padre_id bigint;
    existente record;
    nuevo_id bigint;
    nivel_esperado integer;
BEGIN
    SELECT id INTO STRICT destino FROM sch_admin.tenant
    WHERE codigo = 'FARMALAB' AND estado = 'ACTIVO' AND es_activo = '1';

    FOR fila IN SELECT * FROM categorias_carga ORDER BY padre NULLS FIRST, orden, codigo LOOP
        padre_id := NULL;
        nivel_esperado := CASE WHEN fila.padre IS NULL THEN 1 ELSE 2 END;
        IF fila.padre IS NOT NULL THEN
            SELECT c.id INTO STRICT padre_id FROM sch_catalogo.categoria_producto c
            JOIN categorias_carga s ON s.codigo = fila.padre
            WHERE c.tenant_id = destino AND c.es_activo = '1'
              AND (c.codigo = s.codigo OR lower(c.nombre) = lower(s.nombre));
        END IF;

        SELECT c.* INTO existente FROM sch_catalogo.categoria_producto c
        WHERE c.tenant_id = destino AND c.es_activo = '1'
          AND (c.codigo = fila.codigo OR lower(c.nombre) = lower(fila.nombre));
        IF FOUND THEN
            IF existente.categoria_padre_id IS DISTINCT FROM padre_id
               OR existente.nivel <> nivel_esperado OR existente.estado <> 'ACTIVO'
               OR lower(existente.nombre) <> lower(fila.nombre) THEN
                RAISE EXCEPTION 'Conflicto con categoria existente: %', fila.codigo;
            END IF;
            CONTINUE;
        END IF;

        INSERT INTO sch_catalogo.categoria_producto
            (tenant_id, categoria_padre_id, codigo, nombre, descripcion, nivel, orden, created_by)
        VALUES (destino, padre_id, fila.codigo, fila.nombre, fila.descripcion,
                nivel_esperado, fila.orden, 'SEED_WEB_20261014')
        RETURNING id INTO nuevo_id;
        INSERT INTO categorias_insertadas VALUES (nuevo_id, fila.codigo, fila.nombre);
    END LOOP;
END $$;

SELECT count(*) AS categorias_insertadas FROM categorias_insertadas;
SELECT c.nivel, count(*) AS total
FROM sch_catalogo.categoria_producto c
JOIN sch_admin.tenant t ON t.id = c.tenant_id
WHERE t.codigo = 'FARMALAB' AND c.es_activo = '1'
GROUP BY c.nivel ORDER BY c.nivel;
SELECT * FROM categorias_insertadas ORDER BY id;
COMMIT;
