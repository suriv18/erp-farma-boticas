import { SupportCatalogPage } from '../support-catalog';
import {
  formaFarmaceuticaColumns,
  formaFarmaceuticaFields,
  formaFarmaceuticaResolver,
  formasFarmaceuticasApi
} from '../support-catalog/configs/formas-farmaceuticas.config';

export function FormasFarmaceuticasPage() {
  return (
    <SupportCatalogPage
      title="Formas farmacéuticas"
      description="Administra las formas farmacéuticas del catálogo."
      resourceLabel="forma farmacéutica"
      api={formasFarmaceuticasApi}
      fields={formaFarmaceuticaFields}
      resolver={formaFarmaceuticaResolver}
      columns={formaFarmaceuticaColumns}
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
