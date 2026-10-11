import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router';
import { ListFilters, PageHeader, buttonClassName } from '@boticas/ui-web';
import { SelectField } from '../../../shared/components/FormFields';
import { proveedoresQuery } from '../api/proveedores.api';
import { ESTADOS_PROVEEDOR } from '../api/proveedores.types';
import { ProveedoresTable } from '../components/ProveedoresTable';
import { useProveedoresFiltros } from '../lib/use-proveedores-filtros';

export function ProveedoresPage() {
  const { filtros, setEstado, setTexto, setPage, setSize } = useProveedoresFiltros();
  const { data, isPending, isError } = useQuery(
    proveedoresQuery({
      estado: filtros.estado === '' ? undefined : filtros.estado,
      texto: filtros.texto === '' ? undefined : filtros.texto,
      page: filtros.page,
      size: filtros.size
    })
  );

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Proveedores"
        context="Compras / Proveedores"
        description="Administra los proveedores de la cadena."
        actions={
          <Link to="/compras/proveedores/nuevo" className={buttonClassName()}>
            Nuevo proveedor
          </Link>
        }
      />
      <ListFilters
        label="Buscar proveedor"
        placeholder="Documento, razón social o nombre comercial"
        value={filtros.texto}
        onValueChange={setTexto}
      >
        <div className="w-full sm:w-56">
          <SelectField
            id="filtro-estado"
            label="Estado"
            value={filtros.estado}
            onChange={(event) => setEstado(event.target.value)}
          >
            <option value="">Todos</option>
            {ESTADOS_PROVEEDOR.map((estado) => (
              <option key={estado} value={estado}>
                {estado}
              </option>
            ))}
          </SelectField>
        </div>
      </ListFilters>
      <div className="mt-6">
        <ProveedoresTable
          rows={data?.items ?? []}
          isLoading={isPending}
          isError={isError}
          pagination={{
            page: filtros.page,
            size: filtros.size,
            totalElements: data?.totalElements ?? 0,
            onPageChange: setPage,
            onSizeChange: setSize
          }}
        />
      </div>
    </div>
  );
}
