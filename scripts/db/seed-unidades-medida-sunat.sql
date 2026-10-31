-- Seleccion para botica, consultada el 2026-10-14. No es el catalogo completo.
-- Codigos y denominaciones: SUNAT, RS 169-2015, Anexo 3, Tabla 6.
-- https://www.sunat.gob.pe/orientacion/amazon/ple/RS-169-2015-SUNAT-PLEver5.pdf
-- Simbolos SI para magnitudes; NULL cuando no se consigna un simbolo.
-- permite_decimal es una decision operativa: true para magnitudes continuas,
-- false para unidades contables/empaques. No es un atributo publicado por SUNAT.
-- Catalogo global compartido por tenants. No modifica unidades ya configuradas.
-- Solo corrige la procedencia E2E de NIU si conserva su denominacion verificada.
\set ON_ERROR_STOP on
SET client_encoding = 'UTF8';
BEGIN;
SET LOCAL lock_timeout = '5s';
LOCK TABLE sch_catalogo.unidad_medida IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE unidades_carga (
    codigo varchar(30) PRIMARY KEY,
    denominacion varchar(150) NOT NULL,
    simbolo varchar(30),
    permite_decimal boolean NOT NULL
) ON COMMIT DROP;

INSERT INTO unidades_carga VALUES
('NIU', 'Unidad (bienes)', 'UND', false),
('BX', 'Caja', NULL, false),
('BG', 'Bolsa', NULL, false),
('BO', 'Botellas', NULL, false),
('PK', 'Paquete', NULL, false),
('PR', 'Par', NULL, false),
('DZN', 'Docena', NULL, false),
('CEN', 'Ciento de unidades', NULL, false),
('C62', 'Piezas', NULL, false),
('KT', 'Kit', NULL, false),
('SET', 'Juego', NULL, false),
('TU', 'Tubos', NULL, false),
('MGM', 'Miligramos', 'mg', true),
('GRM', 'Gramo', 'g', true),
('KGM', 'Kilogramo', 'kg', true),
('MLT', 'Mililitro', 'mL', true),
('LTR', 'Litro', 'L', true),
('MTR', 'Metro', 'm', true),
('CMT', 'Centímetro lineal', 'cm', true),
('MMT', 'Milímetro', 'mm', true);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM sch_catalogo.unidad_medida u JOIN unidades_carga s USING (codigo)
        WHERE lower(u.denominacion) <> lower(s.denominacion)
           OR u.estado <> 'ACTIVO' OR u.es_activo <> '1'
    ) THEN
        RAISE EXCEPTION 'Hay unidades existentes con otra denominacion o inactivas. Revisar antes de cargar.';
    END IF;
END $$;

WITH nuevas AS (
    INSERT INTO sch_catalogo.unidad_medida
        (codigo, denominacion, simbolo, permite_decimal, fuente)
    SELECT codigo, denominacion, simbolo, permite_decimal,
           'https://www.sunat.gob.pe/orientacion/amazon/ple/RS-169-2015-SUNAT-PLEver5.pdf#page=57'
    FROM unidades_carga
    ON CONFLICT (codigo) DO NOTHING
    RETURNING codigo
)
SELECT count(*) AS unidades_insertadas FROM nuevas;

UPDATE sch_catalogo.unidad_medida
SET fuente = 'https://www.sunat.gob.pe/orientacion/amazon/ple/RS-169-2015-SUNAT-PLEver5.pdf#page=57'
WHERE codigo = 'NIU' AND denominacion = 'Unidad (bienes)' AND fuente = 'E2E';

SELECT u.codigo, u.denominacion, u.simbolo, u.permite_decimal, u.estado
FROM sch_catalogo.unidad_medida u JOIN unidades_carga s USING (codigo)
ORDER BY u.codigo;
COMMIT;

-- Verificacion posterior al commit en la misma conexion.
SELECT count(*) AS total_unidades, count(*) FILTER (WHERE estado = 'ACTIVO' AND es_activo = '1') AS activas
FROM sch_catalogo.unidad_medida;
