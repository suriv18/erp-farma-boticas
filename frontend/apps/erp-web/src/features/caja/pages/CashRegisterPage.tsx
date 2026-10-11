import { useQuery } from '@tanstack/react-query';
import { Card, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { TerminalSelector, cambiarTerminal, usePuestoTrabajo } from '../../organizacion';
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
        <Card className="mx-auto max-w-md space-y-5 p-6">
          <p className="text-sm font-medium text-neutral-700 dark:text-neutral-200">
            No hay un turno abierto en esta terminal.
          </p>
          <AbrirTurnoForm
            isSubmitting={apertura.isPending}
            error={apertura.mensajeError}
            onSubmit={(valores) => apertura.mutate(valores)}
          />
        </Card>
      );
    }
    return (
      <div className="grid items-start gap-6 lg:grid-cols-2">
        <ResumenTurno turno={turno} />
        <Card className="space-y-4 p-6">
          <h2 className="text-lg font-semibold text-neutral-900 dark:text-neutral-100">
            Arqueo de caja
          </h2>
          <CerrarTurnoForm
            totalEstimado={turno.totalSistema ?? turno.fondoInicial}
            isSubmitting={cierre.isPending}
            error={cierre.mensajeError}
            onSubmit={(valores) => cierre.mutate({ turnoId: turno.id, valores })}
          />
        </Card>
      </div>
    );
  };

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <PageHeader
        title="Caja"
        context="Operaciones / Caja"
        description="Apertura y cierre del turno de la terminal."
      />
      <Card className="p-5">
        <TerminalSelector
          establecimientoId={puesto.establecimientoId}
          terminalId={puesto.terminalId}
          onChange={(establecimientoId, terminalId) =>
            setPuesto(cambiarTerminal(puesto, establecimientoId, terminalId))
          }
        />
      </Card>
      {renderEstado()}
    </div>
  );
}
