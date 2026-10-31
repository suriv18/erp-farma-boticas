import { Button } from '@boticas/ui-web';

export function OptionsFeedback({
  queries
}: {
  queries: { isPending: boolean; isError: boolean; refetch: () => unknown }[];
}) {
  if (queries.some((query) => query.isError))
    return (
      <div role="alert" className="text-danger-700 dark:text-danger-300 space-y-2 text-sm">
        <p>No se pudieron cargar las opciones de algunos campos.</p>
        <Button
          variant="secondary"
          onClick={() =>
            queries
              .filter((query) => query.isError)
              .forEach((query) => {
                void query.refetch();
              })
          }
        >
          Reintentar opciones
        </Button>
      </div>
    );
  return queries.some((query) => query.isPending) ? (
    <p role="status" className="text-sm">
      Cargando opciones…
    </p>
  ) : null;
}
