import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button, Card, EstadoBadge, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { cambiarEstadoEmpresa, empresaQuery } from '../api/empresas.api';
import { ESTADOS_EMPRESA, type EstadoEmpresa } from '../api/empresas.types';
import { invalidateOrganizacion } from '../api/invalidate';
import { Aviso } from '../components/Aviso';
import { CambiarEstadoDialog } from '../components/CambiarEstadoDialog';
import { DatoContacto } from '../components/DatoContacto';
import { DatoItem } from '../components/DatoItem';
import { EmpresaEditarDialog } from '../components/EmpresaEditarDialog';
import { EstablecimientosSection } from '../components/EstablecimientosSection';
import { FormError } from '../components/FormError';
import { motivoSinAltasEmpresa } from '../lib/altas';
import { CONSECUENCIAS_ESTADO_EMPRESA } from '../lib/consecuencias-estado';
import { describeApiError } from '../lib/describe-api-error';
import { valueOrDash, yesNo } from '../lib/format';
import { useRouteParam } from '../lib/use-route-param';
import { useTenantId } from '../lib/use-tenant-id';

export function EmpresaDetailPage() {
  const empresaId = useRouteParam('empresaId');
  const tenantId = useTenantId();
  const queryClient = useQueryClient();
  const [editOpen, setEditOpen] = useState(false);
  const [stateOpen, setStateOpen] = useState(false);

  const result = useQuery({ ...empresaQuery(tenantId, empresaId), enabled: tenantId !== '' });

  const stateMutation = useMutation({
    mutationFn: (estado: EstadoEmpresa) =>
      cambiarEstadoEmpresa(apiClient, empresaId, tenantId, estado),
    onSuccess: () => {
      setStateOpen(false);
      void invalidateOrganizacion(queryClient);
    }
  });

  const closeState = () => {
    setStateOpen(false);
    stateMutation.reset();
  };

  if (result.isPending) {
    return <p className="text-sm text-neutral-500 dark:text-neutral-400">Cargando…</p>;
  }
  if (result.isError) return <FormError message={describeApiError(result.error)} />;

  const empresa = result.data;
  const motivoSinAltas = motivoSinAltasEmpresa(empresa.estado);

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title={empresa.razonSocial}
        context={<Link to="/organizacion/empresas">Organización / Empresas</Link>}
        description={`RUC ${empresa.ruc}`}
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
            <EstadoBadge status={empresa.estado} />
          </DatoItem>
          <DatoItem label="RUC">{empresa.ruc}</DatoItem>
          <DatoItem label="Nombre comercial">{valueOrDash(empresa.nombreComercial)}</DatoItem>
          <DatoItem label="Dirección fiscal">{valueOrDash(empresa.direccionFiscal)}</DatoItem>
          <DatoItem label="Ubigeo fiscal">{valueOrDash(empresa.ubigeoFiscal)}</DatoItem>
          <DatoContacto label="Teléfono" tipo="telefono" valor={empresa.telefono} />
          <DatoContacto label="Correo" tipo="correo" valor={empresa.email} />
          <DatoContacto label="Sitio web" tipo="web" valor={empresa.sitioWeb} />
          <DatoItem label="Moneda funcional">{empresa.monedaFuncional}</DatoItem>
          <DatoItem label="Zona horaria">{empresa.zonaHoraria}</DatoItem>
          <DatoItem label="Venta online">{yesNo(empresa.permiteVentaOnline)}</DatoItem>
        </dl>
      </Card>

      <EstablecimientosSection empresaId={empresaId} motivoSinAltas={motivoSinAltas} />

      {editOpen ? (
        <EmpresaEditarDialog empresa={empresa} onClose={() => setEditOpen(false)} />
      ) : null}

      {stateOpen ? (
        <CambiarEstadoDialog
          title={`Cambiar estado de ${empresa.razonSocial}`}
          estados={ESTADOS_EMPRESA}
          current={empresa.estado}
          consecuencias={CONSECUENCIAS_ESTADO_EMPRESA}
          isSubmitting={stateMutation.isPending}
          error={stateMutation.isError ? describeApiError(stateMutation.error) : null}
          onSubmit={(estado) => stateMutation.mutate(estado)}
          onClose={closeState}
        />
      ) : null}
    </div>
  );
}
