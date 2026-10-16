-- Seleccion de 16 vias del estandar DIGEMID ST_VIA.ADM., septiembre 2026 V.01.
-- Fuente consultada el 2026-10-14:
-- https://www.digemid.minsa.gob.pe/Archivos/PortalWeb/Informativo/Catalogacion/ST_VIA.ADMINISTRACION_2026_V.01.pdf
-- Codigos y denominaciones oficiales. Se preservan los ceros iniciales.
-- Catalogo global compartido por tenants. No modifica productos ni prescripciones.
-- Carga transaccional y repetible; no actualiza registros existentes.
\set ON_ERROR_STOP on
SET client_encoding = 'UTF8';
BEGIN;
SET LOCAL lock_timeout = '5s';
LOCK TABLE sch_catalogo.via_administracion IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE vias_carga (
    codigo varchar(30) PRIMARY KEY,
    denominacion varchar(200) NOT NULL
) ON COMMIT DROP;

INSERT INTO vias_carga VALUES
('054', 'ORAL'),
('002', 'BUCAL'),
('061', 'SUBLINGUAL'),
('066', 'TOPICA'),
('074', 'CUTANEA (DERMICA)'),
('063', 'TRANSDERMICA'),
('067', 'OFTALMICA'),
('073', 'AURICULAR (OTICA)'),
('053', 'NASAL'),
('010', 'INHALATORIA'),
('059', 'RECTAL'),
('065', 'VAGINAL'),
('032', 'INTRAMUSCULAR'),
('048', 'INTRAVENOSA'),
('060', 'SUBCUTANEA'),
('025', 'INTRADERMICA');

CREATE FUNCTION pg_temp.clave_via(text) RETURNS text
LANGUAGE sql IMMUTABLE STRICT AS $$
    SELECT regexp_replace(translate(lower($1), 'áéíóúüñ', 'aeiouun'), '[^a-z0-9]', '', 'g')
$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM sch_catalogo.via_administracion v JOIN vias_carga s
          ON v.codigo = s.codigo OR pg_temp.clave_via(v.denominacion) = pg_temp.clave_via(s.denominacion)
        WHERE v.codigo <> s.codigo
           OR pg_temp.clave_via(v.denominacion) <> pg_temp.clave_via(s.denominacion)
           OR v.estado <> 'ACTIVO' OR v.es_activo <> '1'
    ) THEN
        RAISE EXCEPTION 'Hay una via existente con codigo, denominacion o estado incompatible. Revisar antes de cargar.';
    END IF;
END $$;

WITH nuevas AS (
    INSERT INTO sch_catalogo.via_administracion (codigo, denominacion, fuente)
    SELECT codigo, denominacion,
           'DIGEMID ST_VIA.ADM. Septiembre 2026 V.01 | https://www.digemid.minsa.gob.pe/Archivos/PortalWeb/Informativo/Catalogacion/ST_VIA.ADMINISTRACION_2026_V.01.pdf'
    FROM vias_carga
    ON CONFLICT (codigo) DO NOTHING
    RETURNING codigo
)
SELECT count(*) AS vias_insertadas FROM nuevas;

DO $$
BEGIN
    IF (SELECT count(*) FROM sch_catalogo.via_administracion v
        JOIN vias_carga s USING (codigo)
        WHERE v.estado = 'ACTIVO' AND v.es_activo = '1'
          AND pg_temp.clave_via(v.denominacion) = pg_temp.clave_via(s.denominacion)) <> 16 THEN
        RAISE EXCEPTION 'No se verificaron las 16 vias esperadas';
    END IF;
END $$;
COMMIT;

-- Verificacion persistida posterior al commit.
SELECT codigo, denominacion, estado FROM sch_catalogo.via_administracion
WHERE codigo IN ('054','002','061','066','074','063','067','073','053','010','059','065','032','048','060','025')
ORDER BY denominacion;
SELECT count(*) AS total_vias,
       count(*) FILTER (WHERE estado = 'ACTIVO' AND es_activo = '1') AS activas
FROM sch_catalogo.via_administracion;
