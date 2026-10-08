import { useQuery } from '@tanstack/react-query';
import { DataTable } from '@boticas/ui-web';
import { formatoFechaHora } from '../../../shared/lib/format';
import { recepcionesQuery } from '../api/recepciones.api';
import type { LineaRecepcion, Recepcion } from '../api/recepciones.types';
import { formatoFecha } from '../lib/formato-compras';

type FilaRecepcion = { recepcion: Recepcion; linea: LineaRecepcion };

export function RecepcionesOrden({ ordenId }: { ordenId: string }) {
  const { data, isPending, isError } = useQuery(
    recepcionesQuery({ ordenCompraId: ordenId, size: 100 })
  );
  const filas = (data?.items ?? []).flatMap((recepcion) =>
    recepcion.lineas.map((linea) => ({ recepcion, linea }))
  );

  return (
    <DataTable<FilaRecepcion>
      columns={[
        { header: 'Recepción', cell: ({ recepcion }) => recepcion.numero },
        { header: 'Fecha', cell: ({ recepcion }) => formatoFechaHora(recepcion.fechaRecepcion) },
        { header: 'Línea', cell: ({ linea }) => linea.numeroLineaOrden },
        { header: 'Lote', cell: ({ linea }) => linea.numeroLote },
        { header: 'Vencimiento', cell: ({ linea }) => formatoFecha(linea.fechaVencimiento) },
        { header: 'Recibido', cell: ({ linea }) => linea.cantidadRecibida },
        { header: 'Aceptado', cell: ({ linea }) => linea.cantidadAceptada },
        { header: 'Rechazado', cell: ({ linea }) => linea.cantidadRechazada }
      ]}
      rows={filas}
      rowKey={({ linea }) => linea.id}
      emptyMessage="La orden aún no tiene recepciones."
      isLoading={isPending}
      isError={isError}
      errorMessage="No se pudo cargar las recepciones de la orden."
    />
  );
}
