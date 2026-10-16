-- Catálogo SUNAT 06: Código de tipo de documento de identidad.
-- Fuente: https://cpe.sunat.gob.pe/guias-y-manuales
-- Reglas de validación actualizado al 26.08.2026, hoja Catálogos, A86:B98.
-- Catálogo global: compartido por todos los tenants.

CREATE TABLE sch_catalogo.tipo_documento_identidad (
    codigo          VARCHAR(2) NOT NULL,
    sigla           VARCHAR(30) NOT NULL,
    denominacion    VARCHAR(200) NOT NULL,
    max             SMALLINT,
    min             SMALLINT,
    estado          VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo       CHAR(1) NOT NULL DEFAULT '1',
    CONSTRAINT pk_tipo_documento_identidad PRIMARY KEY (codigo),
    CONSTRAINT ck_tipo_documento_identidad_sigla CHECK (btrim(sigla) <> ''),
    CONSTRAINT ck_tipo_documento_identidad_max CHECK (max IS NULL OR max > 0),
    CONSTRAINT ck_tipo_documento_identidad_min CHECK (min IS NULL OR min > 0),
    CONSTRAINT ck_tipo_documento_identidad_rango CHECK (min IS NULL OR max IS NULL OR min <= max),
    CONSTRAINT ck_tipo_documento_identidad_codigo CHECK (codigo ~ '^[0-9A-Z]$'),
    CONSTRAINT ck_tipo_documento_identidad_denominacion CHECK (btrim(denominacion) <> ''),
    CONSTRAINT ck_tipo_documento_identidad_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_tipo_documento_identidad_es_activo CHECK (es_activo IN ('0', '1'))
);

COMMENT ON TABLE sch_catalogo.tipo_documento_identidad IS
    'Catálogo SUNAT 06 global. La inclusión de un código no implica su aceptación en todos los CPE; validar según comprobante y operación.';
COMMENT ON COLUMN sch_catalogo.tipo_documento_identidad.codigo IS
    'Código alfanumérico SUNAT sin relleno: DNI=1, RUC=6. El código 0 corresponde a DOC.TRIB.NO.DOM.SIN.RUC; no es un valor genérico para clientes sin identificar.';
COMMENT ON COLUMN sch_catalogo.tipo_documento_identidad.sigla IS
    'Abreviatura de presentación definida por la aplicación; no es un código SUNAT adicional.';
COMMENT ON COLUMN sch_catalogo.tipo_documento_identidad.max IS
    'Máximo de caracteres del número de documento; NULL indica sin límite general configurado. Aplicar además las reglas del comprobante y operación.';
COMMENT ON COLUMN sch_catalogo.tipo_documento_identidad.min IS
    'Mínimo de caracteres del número de documento; NULL indica sin límite general configurado, no que se permita un documento vacío.';
COMMENT ON COLUMN sch_catalogo.tipo_documento_identidad.denominacion IS
    'Descripción de la hoja oficial Catálogos, conservando su redacción; espacios finales eliminados.';
COMMENT ON COLUMN sch_catalogo.tipo_documento_identidad.estado IS
    'Estado de disponibilidad local del catálogo (ACTIVO/INACTIVO), fuente de verdad para la aplicación. No sustituye las reglas SUNAT por operación.';
COMMENT ON COLUMN sch_catalogo.tipo_documento_identidad.es_activo IS
    'Disponibilidad local del catálogo replicada en formato legado: 1 activo, 0 inactivo. No sustituye las reglas SUNAT por operación.';

INSERT INTO sch_catalogo.tipo_documento_identidad
    (codigo, sigla, denominacion, max, min, es_activo)
VALUES
    ('0', 'DTSR', 'DOC.TRIB.NO.DOM.SIN.RUC', NULL, NULL, '1'),
    ('1', 'DNI', 'Documento Nacional de Identidad', 8, 8, '1'),
    ('4', 'CE', 'Carnet de extranjería', NULL, NULL, '1'),
    ('6', 'RUC', 'Registro Unico de Contributentes', 11, 11, '1'),
    ('7', 'PAS', 'Pasaporte', NULL, NULL, '1'),
    ('A', 'CDI', 'Cédula Diplomática de identidad', NULL, NULL, '1'),
    ('B', 'DIPR', 'DOC.IDENT.PAIS.RESIDENCIA-NO.D', NULL, NULL, '1'),
    ('C', 'TIN', 'Tax Identification Number - TIN – Doc Trib PP.NN', NULL, NULL, '1'),
    ('D', 'IN', 'Identification Number - IN – Doc Trib PP. JJ', NULL, NULL, '1'),
    ('E', 'TAM', 'TAM- Tarjeta Andina de Migración', NULL, NULL, '1'),
    ('F', 'PTP', 'Permiso Temporal de Permanencia - PTP', NULL, NULL, '1'),
    ('G', 'SC', 'Salvoconducto', NULL, NULL, '1'),
    ('H', 'CPP', 'Carné Permiso Temp.Perman. - CPP', NULL, NULL, '1');
