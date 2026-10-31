ALTER TABLE sch_seguridad.identidad
    ALTER COLUMN tipo_documento TYPE VARCHAR(1);

ALTER TABLE sch_catalogo.tipo_documento_identidad
    ALTER COLUMN codigo TYPE VARCHAR(1);
