import { useQuery } from '@tanstack/react-query';
import { Card, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { TerminalSelector, usePuestoTrabajo } from '../../organizacion';
import { abrirTurno, cerrarTurno, turnoActualQuery } from '../api/turnos.api';
import type { Turno } from '../api/caja.types';
import { AbrirTurnoForm } from '../components/AbrirTurnoForm';
import { CerrarTurnoForm } from '../components/CerrarTurnoForm';
import { ResultadoCierre } from '../components/ResultadoCierre';
import { ResumenTurno } from '../components/ResumenTurno';
import { useMutacionCaja } from '../lib/use-mutacion-caja';
import type { AbrirTurnoFormValues, CerrarTurnoFormValues } from '../schemas/turno.schema';

export function CashRegisterPage() {
  const { puesto, setPuesto } = usePuestoTrabajo();
  const {
    data: turno,
    isPending,
    isError
  } = useQuery({
    ...turnoActualQuery(puesto.terminalId),
    enabled: puesto.terminalId !== ''
  });
  const apertura = useMutacionCaja((valores: AbrirTurnoFormValues) =>
    abrirTurno(apiClient, {
      terminalId: puesto.terminalId,
      fondoInicial: Number(valores.fondoInicial)
    })
  );
  const cierre = useMutacionCaja(
    ({ turnoId, valores }: { turnoId: string; valores: CerrarTurnoFormValues }) =>
      cerrarTurno(apiClient, turnoId, {
        totalDeclarado: Number(valores.totalDeclarado),
        observacion: valores.observacion.trim() === '' ? undefined : valores.observacion.trim()
      })
  );
  const turnoCerrado: Turno | undefined =
    cierre.data?.terminalId === puesto.terminalId ? cierre.data : undefined;

  const renderEstado = () => {
    if (puesto.terminalId === '') return <p>Selecciona una terminal para ver su turno.</p>;
    if (isPending) return <p>Cargando…</p>;
    if (isError) return <p>No se pudo cargar el turno.</p>;
    if (turnoCerrado) return <ResultadoCierre turno={turnoCerrado} />;
    if (turno === null) {
      return (
        <div className="space-y-4">
          <p>No hay un turno abierto en esta terminal.</p>
          <AbrirTurnoForm
            isSubmitting={apertura.isPending}
            error={apertura.mensajeError}
            onSubmit={(valores) => apertura.mutate(valores)}
          />
        </div>
      );
    }
    return (
      <div className="space-y-4">
        <ResumenTurno turno={turno} />
        <CerrarTurnoForm
          totalEstimado={turno.fondoInicial}
          isSubmitting={cierre.isPending}
          error={cierre.mensajeError}
          onSubmit={(valores) => cierre.mutate({ turnoId: turno.id, valores })}
        />
      </div>
    );
  };

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <PageHeader
        title="Caja"
        context="Operaciones / Caja"
        description="Apertura y cierre del turno de la terminal."
      />
      <Card>
        <TerminalSelector
          establecimientoId={puesto.establecimientoId}
          terminalId={puesto.terminalId}
          onChange={(establecimientoId, terminalId) =>
            setPuesto({
              ...puesto,
              establecimientoId,
              terminalId,
              almacenId: establecimientoId === puesto.establecimientoId ? puesto.almacenId : ''
            })
          }
        />
      </Card>
      {renderEstado()}
    </div>
  );
}
