import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button, Card, EstadoBadge, Modal, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import {
  actualizarEstablecimiento,
  cambiarEstadoEstablecimiento,
  establecimientoQuery
} from '../api/establecimientos.api';
import { ESTADOS_ESTABLECIMIENTO, type EstadoEstablecimiento } from '../api/establecimientos.types';
import { invalidateOrganizacion } from '../api/invalidate';
import { AlmacenesSection } from '../components/AlmacenesSection';
import { Aviso } from '../components/Aviso';
import { CambiarEstadoDialog } from '../components/CambiarEstadoDialog';
import { DatoContacto } from '../components/DatoContacto';
import { DatoItem } from '../components/DatoItem';
import { EstablecimientoForm } from '../components/EstablecimientoForm';
import { FormError } from '../components/FormError';
import { TerminalesSection } from '../components/TerminalesSection';
import { CONSECUENCIAS_ESTADO_ESTABLECIMIENTO } from '../lib/consecuencias-estado';
import { motivoSinAltasEstablecimiento } from '../lib/altas';
import { describeApiError } from '../lib/describe-api-error';
import { toEstablecimientoFormValues } from '../lib/form-defaults';
import { toActualizarEstablecimientoPayload } from '../lib/form-payloads';
import { numberOrEmpty } from '../lib/form-values';
import { valueOrDash, yesNo } from '../lib/format';
import { useRouteParam } from '../lib/use-route-param';
import { useTenantId } from '../lib/use-tenant-id';
import type { EstablecimientoFormValues } from '../schemas/establecimiento.schema';

export function EstablecimientoDetailPage() {
  const establecimientoId = useRouteParam('establecimientoId');
  const tenantId = useTenantId();
  const queryClient = useQueryClient();
  const [editOpen, setEditOpen] = useState(false);
  const [stateOpen, setStateOpen] = useState(false);

  const result = useQuery({
    ...establecimientoQuery(tenantId, establecimientoId),
    enabled: tenantId !== ''
  });

  const updateMutation = useMutation({
    mutationFn: (values: EstablecimientoFormValues) =>
      actualizarEstablecimiento(
        apiClient,
        establecimientoId,
        tenantId,
        toActualizarEstablecimientoPayload(values)
      ),
    onSuccess: () => {
      setEditOpen(false);
      void invalidateOrganizacion(queryClient);
    }
  });

  const stateMutation = useMutation({
    mutationFn: (estado: EstadoEstablecimiento) =>
      cambiarEstadoEstablecimiento(apiClient, establecimientoId, tenantId, estado),
    onSuccess: () => {
      setStateOpen(false);
      void invalidateOrganizacion(queryClient);
    }
  });

  const closeEdit = () => {
    setEditOpen(false);
    updateMutation.reset();
  };

  const closeState = () => {
    setStateOpen(false);
    stateMutation.reset();
  };

  if (result.isPending) {
    return <p className="text-sm text-neutral-500 dark:text-neutral-400">Cargando…</p>;
  }
  if (result.isError) return <FormError message={describeApiError(result.error)} />;

  const establecimiento = result.data;
  const motivoSinAltas = motivoSinAltasEstablecimiento(establecimiento.estadoOperativo);

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title={establecimiento.nombre}
        context={
          <Link to={`/organizacion/empresas/${establecimiento.empresaId}`}>
            Organización / Empresa
          </Link>
        }
        description={`Código ${establecimiento.codigo}`}
        actions={
          <>
            <Button variant="secondary" onClick={() => setEditOpen(true)}>
              Editar
            </Button>
            <Button variant="secondary" onClick={() => setStateOpen(true)}>
              Cambiar estado
            </Button>
          </>
        }
      />

      {motivoSinAltas ? <Aviso>{motivoSinAltas}</Aviso> : null}

      <Card className="mt-6 p-6">
        <dl className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          <DatoItem label="Estado">
            <EstadoBadge status={establecimiento.estadoOperativo} />
          </DatoItem>
          <DatoItem label="Código">{establecimiento.codigo}</DatoItem>
          <DatoItem label="Tipo">{establecimiento.tipoEstablecimiento}</DatoItem>
          <DatoItem label="Perfil de operación">{establecimiento.perfilOperacion}</DatoItem>
          <DatoItem label="Anexo SUNAT">{establecimiento.codigoAnexoSunat}</DatoItem>
          <DatoItem label="Código DIGEMID">{valueOrDash(establecimiento.codigoDigemid)}</DatoItem>
          <DatoItem label="Categoría regulatoria">
            {valueOrDash(establecimiento.categoriaRegulatoriaCodigo)}
          </DatoItem>
          <DatoItem label="Dirección">{valueOrDash(establecimiento.direccion)}</DatoItem>
          <DatoItem label="Ubigeo">{valueOrDash(establecimiento.ubigeo)}</DatoItem>
          <DatoItem label="Referencia">{valueOrDash(establecimiento.referencia)}</DatoItem>
          <DatoItem label="Latitud">{valueOrDash(numberOrEmpty(establecimiento.latitud))}</DatoItem>
          <DatoItem label="Longitud">
            {valueOrDash(numberOrEmpty(establecimiento.longitud))}
          </DatoItem>
          <DatoContacto label="Teléfono" tipo="telefono" valor={establecimiento.telefono} />
          <DatoContacto label="Correo" tipo="correo" valor={establecimiento.email} />
          <DatoItem label="Zona horaria">{establecimiento.zonaHoraria}</DatoItem>
          <DatoItem label="Es principal">{yesNo(establecimiento.esPrincipal)}</DatoItem>
          <DatoItem label="Venta online">{yesNo(establecimiento.permiteVentaOnline)}</DatoItem>
          <DatoItem label="Delivery">{yesNo(establecimiento.permiteDelivery)}</DatoItem>
        </dl>
      </Card>

      <AlmacenesSection establecimientoId={establecimientoId} motivoSinAltas={motivoSinAltas} />
      <TerminalesSection establecimientoId={establecimientoId} motivoSinAltas={motivoSinAltas} />

      <Modal open={editOpen} onClose={closeEdit} title="Editar establecimiento" size="lg">
        <EstablecimientoForm
          isEdit
          defaultValues={toEstablecimientoFormValues(establecimiento)}
          submitLabel="Guardar cambios"
          isSubmitting={updateMutation.isPending}
          error={updateMutation.isError ? describeApiError(updateMutation.error) : null}
          onSubmit={(values) => updateMutation.mutate(values)}
        />
      </Modal>

      {stateOpen ? (
        <CambiarEstadoDialog
          title={`Cambiar estado de ${establecimiento.nombre}`}
          estados={ESTADOS_ESTABLECIMIENTO}
          current={establecimiento.estadoOperativo}
          consecuencias={CONSECUENCIAS_ESTADO_ESTABLECIMIENTO}
          isSubmitting={stateMutation.isPending}
          error={stateMutation.isError ? describeApiError(stateMutation.error) : null}
          onSubmit={(estado) => stateMutation.mutate(estado)}
          onClose={closeState}
        />
      ) : null}
    </div>
  );
}
