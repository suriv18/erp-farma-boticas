import { SupportCatalogPage } from '../support-catalog';
import {
  unidadMedidaColumns,
  unidadMedidaFields,
  unidadMedidaResolver,
  unidadesMedidaApi
} from '../support-catalog/configs/unidades-medida.config';

export function UnidadesMedidaPage() {
  return (
    <SupportCatalogPage
      title="Unidades de medida"
      description="Administra las unidades de medida del catálogo."
      resourceLabel="unidad de medida"
      api={unidadesMedidaApi}
      fields={unidadMedidaFields}
      resolver={unidadMedidaResolver}
      columns={unidadMedidaColumns}
      searchableFields={['codigo', 'denominacion']}
      toRequest={(values) => values}
      toDefaultValues={(item) => ({
        codigo: item.codigo,
        denominacion: item.denominacion,
        simbolo: item.simbolo ?? '',
        permiteDecimal: item.permiteDecimal,
        fuente: item.fuente ?? ''
      })}
    />
  );
}
