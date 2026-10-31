import { useEffect } from 'react';
import { FormErrorSummary } from '../../../shared/components/FormErrorSummary';
import { OptionsFeedback } from './OptionsFeedback';
import { CatalogFormActions } from './CatalogFormActions';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { PRODUCTO_REGULADO_VACIO } from '../lib/producto-regulado-form';
import { useSoporteOpcionesQuery } from '../lib/use-opciones';
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
  onCancel?: (() => void) | undefined;
  isSubmitting?: boolean;
  fullPage?: boolean;
  onDirtyChange?: (dirty: boolean) => void;
  error?: string | null;
};

export function ProductoReguladoForm({
  defaultValues = PRODUCTO_REGULADO_VACIO,
  onSubmit,
  submitLabel,
  onCancel,
  fullPage = false,
  onDirtyChange,
  isSubmitting = false,
  error = null
}: ProductoReguladoFormProps) {
  const formasQuery = useSoporteOpcionesQuery(formasFarmaceuticasApi);
  const formas = formasQuery.data ?? [];
  const viasQuery = useSoporteOpcionesQuery(viasAdministracionApi);
  const vias = viasQuery.data ?? [];
  const unidadesQuery = useSoporteOpcionesQuery(unidadesMedidaApi);
  const unidades = unidadesQuery.data ?? [];
  const condicionesQuery = useSoporteOpcionesQuery(condicionesVentaApi);
  const condiciones = condicionesQuery.data ?? [];
  const clasificacionesQuery = useSoporteOpcionesQuery(clasificacionesControladasApi);
  const clasificaciones = clasificacionesQuery.data ?? [];

  const {
    formState: { errors, isDirty, submitCount },
    handleSubmit,
    register
  } = useForm<ProductoReguladoFormValues>({
    defaultValues,
    mode: 'onTouched',
    resolver: zodResolver(productoReguladoSchema)
  });

  useEffect(() => {
    onDirtyChange?.(isDirty);
  }, [isDirty, onDirtyChange]);

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
      <p className="text-sm leading-6 text-neutral-500 dark:text-neutral-400">
        El tipo de producto y la denominación son obligatorios. Si informas un registro sanitario,
        completa tanto el tipo como el número. Los demás datos son opcionales.
      </p>
      <OptionsFeedback
        queries={[formasQuery, viasQuery, unidadesQuery, condicionesQuery, clasificacionesQuery]}
      />
      {submitCount > 0 ? <FormErrorSummary errors={errors} prefix="producto-regulado" /> : null}
      {grupos.map((grupo) => (
        <fieldset
          key={grupo.titulo}
          className="rounded-xl border border-neutral-200 p-4 sm:p-5 dark:border-neutral-700"
        >
          <legend className="text-primary-700 dark:text-primary-300 px-2 text-sm font-bold">
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
      <CatalogFormActions
        submitLabel={submitLabel}
        isSubmitting={isSubmitting}
        error={error}
        onCancel={onCancel}
        fullPage={fullPage}
      />
    </form>
  );
}
