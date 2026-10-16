-- Marcas reales consultadas el 2026-10-14. Seleccion inicial para FARMALAB.
-- Fuente comprobable por marca en marcas_carga; no implica titularidad ni registro sanitario.
-- Nombres normalizados en capitalizacion y puntuacion (Oral-B, La Roche-Posay).
-- Codigos internos propios. Descripcion NULL: no se inventan datos del fabricante.
-- Carga transaccional, repetible, sin actualizar ni eliminar marcas existentes.
\set ON_ERROR_STOP on
SET client_encoding = 'UTF8';
BEGIN;
SET LOCAL lock_timeout = '5s';
LOCK TABLE sch_catalogo.marca IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE marcas_carga (
    codigo varchar(50) PRIMARY KEY,
    nombre varchar(180) NOT NULL,
    fuente text NOT NULL
) ON COMMIT DROP;

INSERT INTO marcas_carga VALUES
('MAR-COLGATE', 'Colgate', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-ORAL-B', 'Oral-B', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-SENSODYNE', 'Sensodyne', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-LISTERINE', 'Listerine', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-AQUAFRESH', 'Aquafresh', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-DENTO', 'Dento', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-KOLYNOS', 'Kolynos', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-COREGA', 'Corega', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-VITIS', 'Vitis', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-WATERPIK', 'Waterpik', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-DOVE', 'Dove', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-PANTENE', 'Pantene', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-SEDAL', 'Sedal', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-SAVITAL', 'Savital', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-HEAD-SHOULDERS', 'Head & Shoulders', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-TIO-NACHO', 'Tío Nacho', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-NIVEA', 'Nivea', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-GARNIER', 'Garnier', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-REXONA', 'Rexona', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-GILLETTE', 'Gillette', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-SCHICK', 'Schick', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-OLD-SPICE', 'Old Spice', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-NOSOTRAS', 'Nosotras', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-KOTEX', 'Kotex', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-ALWAYS', 'Always', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-LADYSOFT', 'Ladysoft', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-LACTACYD', 'Lactacyd', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-BIODERMA', 'Bioderma', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-CERAVE', 'CeraVe', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-EUCERIN', 'Eucerin', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-ISDIN', 'ISDIN', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-LA-ROCHE-POSAY', 'La Roche-Posay', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-CETAPHIL', 'Cetaphil', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-NEUTROGENA', 'Neutrogena', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-URIAGE', 'Uriage', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-CICATRICURE', 'Cicatricure', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-HUGGIES', 'Huggies', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-PAMPERS', 'Pampers', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-BABYSEC', 'Babysec', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-PEQUENIN', 'Pequeñín', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-TENA', 'TENA', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-PLENITUD', 'Plenitud', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-GLUCERNA', 'Glucerna', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-PEDIASURE', 'PediaSure', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-ENFAGROW', 'Enfagrow', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-SIMILAC', 'Similac', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-DUREX', 'Durex', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-CLEARBLUE', 'Clearblue', 'https://www.boticasperu.pe/cuidado_personal/corporal'),
('MAR-PANADOL', 'Panadol', 'https://www.panadol.com/es-pe/productos/dolor-de-cabeza/panadol-tabletas/'),
('MAR-BEPANTHEN', 'Bepanthen', 'https://www.bepanthen.com.pe/productos/bepanthen-crema/');

CREATE FUNCTION pg_temp.clave_marca(text) RETURNS text
LANGUAGE sql IMMUTABLE STRICT AS $$
    SELECT regexp_replace(
        translate(lower($1), 'áéíóúüñ', 'aeiouun'),
        '[^a-z0-9]', '', 'g')
$$;

CREATE TEMP TABLE marcas_insertadas (id bigint, codigo text, nombre text) ON COMMIT DROP;
DO $$
DECLARE
    destino bigint;
    fila record;
    coincidencias integer;
    existente record;
    nuevo_id bigint;
BEGIN
    SELECT id INTO STRICT destino FROM sch_admin.tenant
    WHERE codigo = 'FARMALAB' AND estado = 'ACTIVO' AND es_activo = '1';

    FOR fila IN SELECT * FROM marcas_carga ORDER BY nombre LOOP
        SELECT count(*) INTO coincidencias FROM sch_catalogo.marca m
        WHERE m.tenant_id = destino AND m.es_activo = '1'
          AND (m.codigo = fila.codigo OR pg_temp.clave_marca(m.nombre) = pg_temp.clave_marca(fila.nombre));
        IF coincidencias > 1 THEN
            RAISE EXCEPTION 'Multiples coincidencias para la marca %', fila.nombre;
        ELSIF coincidencias = 1 THEN
            SELECT m.* INTO STRICT existente FROM sch_catalogo.marca m
            WHERE m.tenant_id = destino AND m.es_activo = '1'
              AND (m.codigo = fila.codigo OR pg_temp.clave_marca(m.nombre) = pg_temp.clave_marca(fila.nombre));
            IF pg_temp.clave_marca(existente.nombre) <> pg_temp.clave_marca(fila.nombre)
               OR existente.estado <> 'ACTIVO' THEN
                RAISE EXCEPTION 'Conflicto con marca existente %', fila.nombre;
            END IF;
            CONTINUE;
        END IF;

        INSERT INTO sch_catalogo.marca (tenant_id, codigo, nombre, descripcion, created_by)
        VALUES (destino, fila.codigo, fila.nombre, NULL, 'SEED_MARCAS_WEB_20261014')
        RETURNING id INTO nuevo_id;
        INSERT INTO marcas_insertadas VALUES (nuevo_id, fila.codigo, fila.nombre);
    END LOOP;

    IF (SELECT count(*) FROM marcas_carga s WHERE EXISTS (
        SELECT 1 FROM sch_catalogo.marca m
        WHERE m.tenant_id = destino AND m.estado = 'ACTIVO' AND m.es_activo = '1'
          AND pg_temp.clave_marca(m.nombre) = pg_temp.clave_marca(s.nombre)
    )) <> 50 THEN
        RAISE EXCEPTION 'No se verificaron las 50 marcas esperadas';
    END IF;
END $$;

SELECT count(*) AS marcas_insertadas FROM marcas_insertadas;
SELECT codigo, nombre FROM marcas_insertadas ORDER BY nombre;
COMMIT;

-- Verificacion persistida posterior al commit.
SELECT count(*) AS total_marcas,
       count(*) FILTER (WHERE m.estado = 'ACTIVO' AND m.es_activo = '1') AS activas,
       count(DISTINCT pg_temp.clave_marca(m.nombre)) AS nombres_distintos
FROM sch_catalogo.marca m JOIN sch_admin.tenant t ON t.id = m.tenant_id
WHERE t.codigo = 'FARMALAB';

