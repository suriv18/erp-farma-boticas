import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router';
import { Button, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { FormError } from '../../../shared/components/FormError';
import { skuQuery, type SkuResumen } from '../../catalogo';
import { useEstablecimientos } from '../../organizacion';
import { crearOrden } from '../api/ordenes.api';
import type { CrearOrdenPayload } from '../api/ordenes.types';
import { proveedoresQuery } from '../api/proveedores.api';
import { BuscadorSku } from '../components/BuscadorSku';
import { LineasEditor } from '../components/LineasEditor';
import { OrdenCabeceraForm } from '../components/OrdenCabeceraForm';
import { TotalesOrden } from '../components/TotalesOrden';
import {
  actualizarLinea,
  agregarSku,
  quitarLinea,
  restablecerImpuesto,
  totalesOrden,
  type LineaBorrador
} from '../lib/orden-calculo';
import {
  CABECERA_VACIA,
  cabeceraDesdeProveedor,
  erroresCabecera,
  fechaLocalISO,
  puedeCrear,
  toCrearOrdenPayload,
  type CabeceraOrden
} from '../lib/orden-cabecera';
import { useMutacionCompras } from '../lib/use-mutacion-compras';

export function NuevaOrdenPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const establecimientos = useEstablecimientos();
  const { data: proveedores } = useQuery(proveedoresQuery({ estado: 'ACTIVO', size: 100 }));
  const [cabecera, setCabecera] = useState<CabeceraOrden>(CABECERA_VACIA);
  const [lineas, setLineas] = useState<LineaBorrador[]>([]);
  const [intentado, setIntentado] = useState(false);
  const [errorSku, setErrorSku] = useState<string | null>(null);
  const hoy = fechaLocalISO();
  const listaProveedores = proveedores?.items ?? [];
  const creacion = useMutacionCompras(
    (payload: CrearOrdenPayload) => crearOrden(apiClient, payload),
    (orden) => {
      void navigate(`/compras/ordenes/${orden.id}`);
    }
  );

  const cambiarCabecera = (campo: keyof CabeceraOrden, valor: string) => {
    const proveedor =
      campo === 'proveedorId' ? listaProveedores.find(({ id }) => id === valor) : undefined;
    setCabecera((actual) =>
      proveedor ? cabeceraDesdeProveedor(actual, proveedor) : { ...actual, [campo]: valor }
    );
  };

  const elegirSku = async (sku: SkuResumen) => {
    setErrorSku(null);
    const detalle = await queryClient.fetchQuery(skuQuery(sku.id)).catch(() => undefined);
    if (detalle === undefined) {
      setErrorSku('No se pudo leer el producto. Inténtalo de nuevo.');
      return;
    }
    setLineas((actuales) => agregarSku(actuales, sku, detalle.afectoIgv));
  };

  const crear = () => {
    setIntentado(true);
    if (puedeCrear(cabecera, lineas, hoy)) creacion.mutate(toCrearOrdenPayload(cabecera, lineas));
  };

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <PageHeader
        title="Nueva orden de compra"
        context={<Link to="/compras/ordenes">Compras / Órdenes</Link>}
        description="Registra una orden en borrador para un proveedor."
      />
      <OrdenCabeceraForm
        valores={cabecera}
        errores={intentado ? erroresCabecera(cabecera, hoy) : {}}
        proveedores={listaProveedores}
        establecimientos={establecimientos}
        onCambiar={cambiarCabecera}
      />
      <section className="space-y-4">
        <h2 className="text-base font-semibold">Productos</h2>
        <BuscadorSku
          onElegir={(sku) => {
            void elegirSku(sku);
          }}
        />
        {errorSku ? <FormError message={errorSku} /> : null}
        <LineasEditor
          lineas={lineas}
          moneda={cabecera.moneda}
          mostrarErrores={intentado}
          onCambiar={(skuId, cambios) =>
            setLineas((actuales) => actualizarLinea(actuales, skuId, cambios))
          }
          onQuitar={(skuId) => setLineas((actuales) => quitarLinea(actuales, skuId))}
          onRestablecerImpuesto={(skuId) =>
            setLineas((actuales) => restablecerImpuesto(actuales, skuId))
          }
        />
        {intentado && lineas.length === 0 ? (
          <FormError message="Agrega al menos un producto." />
        ) : null}
      </section>
      <TotalesOrden totales={totalesOrden(lineas)} moneda={cabecera.moneda} />
      {creacion.mensajeError ? <FormError message={creacion.mensajeError} /> : null}
      <div className="flex flex-wrap gap-3">
        <Button disabled={creacion.isPending} onClick={crear}>
          Crear orden
        </Button>
        <Button variant="secondary" onClick={() => void navigate('/compras/ordenes')}>
          Cancelar
        </Button>
      </div>
    </div>
  );
}
