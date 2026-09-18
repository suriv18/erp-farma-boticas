import type { Permiso } from '../api/permisos.types';

export type PermisosChecklistProps = {
  permisos: Permiso[];
  selectedCodes: Set<string>;
  onChange: (codes: Set<string>) => void;
};

function groupByModule(permisos: Permiso[]): Map<string, Permiso[]> {
  const groups = new Map<string, Permiso[]>();
  for (const permiso of permisos) {
    const existing = groups.get(permiso.moduleName) ?? [];
    existing.push(permiso);
    groups.set(permiso.moduleName, existing);
  }
  return groups;
}

export function PermisosChecklist({ permisos, selectedCodes, onChange }: PermisosChecklistProps) {
  const groups = groupByModule(permisos);

  function toggle(code: string) {
    const next = new Set(selectedCodes);
    if (next.has(code)) next.delete(code);
    else next.add(code);
    onChange(next);
  }

  return (
    <div className="max-h-80 space-y-5 overflow-y-auto rounded-xl border border-slate-200 p-4">
      {[...groups.entries()].map(([moduleName, modulePermisos]) => (
        <div key={moduleName}>
          <p className="text-xs font-bold tracking-wide text-slate-500 uppercase">{moduleName}</p>
          <div className="mt-2 space-y-2">
            {modulePermisos.map((permiso) => (
              <label key={permiso.code} className="flex items-center gap-2.5 text-sm text-slate-700">
                <input
                  type="checkbox"
                  aria-label={permiso.name}
                  checked={selectedCodes.has(permiso.code)}
                  onChange={() => toggle(permiso.code)}
                  className="size-4 rounded border-slate-300 text-teal-700 focus:ring-teal-600"
                />
                {permiso.name}
              </label>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
