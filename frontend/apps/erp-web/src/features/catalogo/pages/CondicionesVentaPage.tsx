import { SupportCatalogPage } from '../support-catalog';
import {
  condicionVentaColumns,
  condicionVentaFields,
  condicionVentaResolver,
  condicionesVentaApi
} from '../support-catalog/configs/condiciones-venta.config';

export function CondicionesVentaPage() {
  return (
    <SupportCatalogPage
      title="Condiciones de venta"
      description="Administra las condiciones de venta del catálogo."
      resourceLabel="condición de venta"
      api={condicionesVentaApi}
      fields={condicionVentaFields}
      resolver={condicionVentaResolver}
      columns={condicionVentaColumns}
      searchableFields={['codigo', 'denominacion']}
      toRequest={(values) => values}
      toDefaultValues={(item) => ({
        codigo: item.codigo,
        denominacion: item.denominacion,
        requiereReceta: item.requiereReceta,
        requiereRetencion: item.requiereRetencion,
        fuente: item.fuente ?? '',
        versionFuente: item.versionFuente ?? ''
      })}
    />
  );
}
