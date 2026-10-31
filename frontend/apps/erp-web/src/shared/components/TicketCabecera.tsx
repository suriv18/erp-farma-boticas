type TicketCabeceraProps = {
  titulo: string;
  insignia: string;
  etiqueta: string;
  valor: string;
};

export function TicketCabecera({ titulo, insignia, etiqueta, valor }: TicketCabeceraProps) {
  return (
    <>
      <div className="bg-primary-900 px-5 pt-4 pb-6 text-white">
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-sm font-semibold">{titulo}</h2>
          <span className="bg-primary-800 text-primary-100 rounded-full px-2.5 py-0.5 text-xs font-medium">
            {insignia}
          </span>
        </div>
        <dl className="mt-4">
          <dt className="text-primary-200 text-xs font-semibold tracking-wide uppercase">
            {etiqueta}
          </dt>
          <dd className="mt-1 text-5xl font-semibold tracking-tight tabular-nums">{valor}</dd>
        </dl>
      </div>
      <div className="border-t-2 border-dashed border-neutral-300 dark:border-neutral-600" />
    </>
  );
}
