import { SupportCatalogPage } from '../support-catalog';
import {
  viaAdministracionColumns,
  viaAdministracionFields,
  viaAdministracionResolver,
  viasAdministracionApi
} from '../support-catalog/configs/vias-administracion.config';

export function ViasAdministracionPage() {
  return (
    <SupportCatalogPage
      title="Vías de administración"
      description="Administra las vías de administración del catálogo."
      resourceLabel="vía de administración"
      api={viasAdministracionApi}
      fields={viaAdministracionFields}
      resolver={viaAdministracionResolver}
      columns={viaAdministracionColumns}
      searchableFields={['codigo', 'denominacion']}
      toRequest={(values) => values}
      toDefaultValues={(item) => ({
        codigo: item.codigo,
        denominacion: item.denominacion,
        fuente: item.fuente ?? ''
      })}
    />
  );
}
