import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { FormError } from '../../../shared/components/FormError';
import { PRODUCTO_REGULADO_VACIO } from '../lib/producto-regulado-form';
import { useOpcionesSoporte } from '../lib/use-opciones';
import {
  productoReguladoSchema,
  type ProductoReguladoFormValues
} from '../schemas/producto-regulado.schema';
import { clasificacionesControladasApi } from '../support-catalog/configs/clasificaciones-controladas.config';
import { condicionesVentaApi } from '../support-catalog/configs/condiciones-venta.config';
import { formasFarmaceuticasApi } from '../support-catalog/configs/formas-farmaceuticas.config';
import { unidadesMedidaApi } from '../support-catalog/configs/unidades-medida.config';
import { viasAdministracionApi } from '../support-catalog/configs/vias-administracion.config';
import { CamposFormulario, type CampoFormulario } from './CamposFormulario';

type Grupo = { titulo: string; campos: CampoFormulario<ProductoReguladoFormValues>[] };

export type ProductoReguladoFormProps = {
  defaultValues?: ProductoReguladoFormValues;
  onSubmit: (values: ProductoReguladoFormValues) => void;
  submitLabel: string;
  isSubmitting?: boolean;
  error?: string | null;
};

export function ProductoReguladoForm({
  defaultValues = PRODUCTO_REGULADO_VACIO,
  onSubmit,
  submitLabel,
  isSubmitting = false,
  error = null
}: ProductoReguladoFormProps) {
  const formas = useOpcionesSoporte(formasFarmaceuticasApi);
  const vias = useOpcionesSoporte(viasAdministracionApi);
  const unidades = useOpcionesSoporte(unidadesMedidaApi);
  const condiciones = useOpcionesSoporte(condicionesVentaApi);
  const clasificaciones = useOpcionesSoporte(clasificacionesControladasApi);

  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<ProductoReguladoFormValues>({
    defaultValues,
    mode: 'onTouched',
    resolver: zodResolver(productoReguladoSchema)
  });

  const grupos: Grupo[] = [
    {
      titulo: 'Identificación',
      campos: [
        { name: 'tipoProducto', label: 'Tipo de producto', tipo: 'texto' },
        { name: 'denominacion', label: 'Denominación', tipo: 'texto' },
        { name: 'tipoRegistro', label: 'Tipo de registro', tipo: 'texto' },
        { name: 'numeroRegistro', label: 'Número de registro', tipo: 'texto' },
        { name: 'rubroCodigo', label: 'Rubro', tipo: 'texto' },
        { name: 'concentracionTexto', label: 'Concentración', tipo: 'texto' },
        { name: 'presentacionRegulatoria', label: 'Presentación regulatoria', tipo: 'texto' }
      ]
    },
    {
      titulo: 'Clasificación',
      campos: [
        {
          name: 'formaFarmaceuticaCodigo',
          label: 'Forma farmacéutica',
          tipo: 'seleccion',
          opciones: formas
        },
        {
          name: 'viaAdministracionCodigo',
          label: 'Vía de administración',
          tipo: 'seleccion',
          opciones: vias
        },
        {
          name: 'unidadMedidaCodigo',
          label: 'Unidad de medida',
          tipo: 'seleccion',
          opciones: unidades
        },
        {
          name: 'condicionVentaCodigo',
          label: 'Condición de venta',
          tipo: 'seleccion',
          opciones: condiciones
        },
        {
          name: 'clasificacionControladaCodigo',
          label: 'Clasificación controlada',
          tipo: 'seleccion',
          opciones: clasificaciones
        },
        { name: 'clasificacionAtc', label: 'Clasificación ATC', tipo: 'texto' }
      ]
    },
    {
      titulo: 'Origen y titularidad',
      campos: [
        { name: 'tipoLiberacion', label: 'Tipo de liberación', tipo: 'texto' },
        { name: 'origenFabricacion', label: 'Origen de fabricación', tipo: 'texto' },
        { name: 'paisOrigen', label: 'País de origen', tipo: 'texto' },
        { name: 'subpartidaNacional', label: 'Subpartida nacional', tipo: 'texto' },
        { name: 'titularRegistro', label: 'Titular de registro', tipo: 'texto' },
        { name: 'fabricante', label: 'Fabricante', tipo: 'texto' },
        { name: 'importador', label: 'Importador', tipo: 'texto' },
        { name: 'establecimientoExpendio', label: 'Establecimiento de expendio', tipo: 'texto' }
      ]
    },
    {
      titulo: 'Vigencia y fuente',
      campos: [
        { name: 'vigenteDesde', label: 'Vigente desde', tipo: 'fecha' },
        { name: 'vigenteHasta', label: 'Vigente hasta', tipo: 'fecha' },
        { name: 'fuente', label: 'Fuente', tipo: 'texto' },
        { name: 'versionFuente', label: 'Versión de fuente', tipo: 'texto' }
      ]
    }
  ];

  return (
    <form
      className="space-y-6"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      {grupos.map((grupo) => (
        <fieldset key={grupo.titulo} className="space-y-4">
          <legend className="text-sm font-bold text-neutral-900 dark:text-neutral-50">
            {grupo.titulo}
          </legend>
          <div className="grid gap-4 sm:grid-cols-2">
            <CamposFormulario
              prefijo="producto-regulado"
              campos={grupo.campos}
              register={register}
              errors={errors}
            />
          </div>
        </fieldset>
      ))}
      {error ? <FormError message={error} /> : null}
      <Button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
}
