import { SupportCatalogPage } from '../support-catalog';
import {
  clasificacionControladaColumns,
  clasificacionControladaFields,
  clasificacionControladaResolver,
  clasificacionesControladasApi
} from '../support-catalog/configs/clasificaciones-controladas.config';

export function ClasificacionesControladasPage() {
  return (
    <SupportCatalogPage
      title="Clasificaciones controladas"
      description="Administra las clasificaciones controladas del catálogo."
      resourceLabel="clasificación controlada"
      api={clasificacionesControladasApi}
      fields={clasificacionControladaFields}
      resolver={clasificacionControladaResolver}
      columns={clasificacionControladaColumns}
      searchableFields={['codigo', 'denominacion']}
      toRequest={(values) => values}
      toDefaultValues={(item) => ({
        codigo: item.codigo,
        denominacion: item.denominacion,
        normaFuente: item.normaFuente ?? '',
        requiereRecetaEspecial: item.requiereRecetaEspecial,
        retieneReceta: item.retieneReceta,
        vigenciaRecetaDias: item.vigenciaRecetaDias ?? undefined
      })}
    />
  );
}
