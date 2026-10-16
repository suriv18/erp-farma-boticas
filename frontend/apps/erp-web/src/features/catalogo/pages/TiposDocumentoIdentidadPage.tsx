import { SupportCatalogPage } from '../support-catalog';
import {
  tipoDocumentoIdentidadColumns,
  tipoDocumentoIdentidadFields,
  tipoDocumentoIdentidadResolver,
  tiposDocumentoIdentidadApi
} from '../support-catalog/configs/tipos-documento-identidad.config';

export function TiposDocumentoIdentidadPage() {
  return (
    <SupportCatalogPage
      title="Tipos de documento de identidad"
      description="Administra el catálogo SUNAT de tipos de documento de identidad."
      resourceLabel="tipo de documento"
      api={tiposDocumentoIdentidadApi}
      fields={tipoDocumentoIdentidadFields}
      resolver={tipoDocumentoIdentidadResolver}
      columns={tipoDocumentoIdentidadColumns}
      searchableFields={['codigo', 'sigla', 'denominacion']}
      toRequest={(values) => values}
      toDefaultValues={(item) => ({
        codigo: item.codigo,
        sigla: item.sigla,
        denominacion: item.denominacion,
        max: item.max ?? undefined,
        min: item.min ?? undefined
      })}
    />
  );
}
