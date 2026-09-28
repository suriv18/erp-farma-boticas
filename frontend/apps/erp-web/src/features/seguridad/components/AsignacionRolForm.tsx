import { useEffect } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { Button, FormField } from '@boticas/ui-web';
import { rolesQuery } from '../api/roles.api';
import { corporateStructureQuery } from '../../organizacion';
import {
  asignacionRolSchema,
  SCOPE_TYPES,
  type AsignacionRolFormValues
} from '../schemas/asignacion-rol.schema';

export type AsignacionRolFormProps = {
  tenantId: string;
  onSubmit: (values: AsignacionRolFormValues) => void;
  onCancel: () => void;
  isSubmitting?: boolean;
  errorMessage?: string | undefined;
  formId?: string | undefined;
};

export function AsignacionRolForm({
  tenantId,
  onSubmit,
  onCancel,
  isSubmitting = false,
  errorMessage,
  formId
}: AsignacionRolFormProps) {
  const rolesResult = useQuery(rolesQuery({ tenantId, size: 100 }));
  const structureResult = useQuery(corporateStructureQuery);

  const {
    formState: { errors },
    handleSubmit,
    register,
    setValue,
    watch
  } = useForm<AsignacionRolFormValues>({
    defaultValues: { roleId: '', scopeType: 'ESTABLECIMIENTO' },
    mode: 'onTouched',
    resolver: zodResolver(asignacionRolSchema)
  });

  const scopeType = watch('scopeType');
  const companyId = watch('companyId');
  const establishmentId = watch('establishmentId');

  const companies = structureResult.data?.companies ?? [];
  const selectedCompany = companies.find((company) => company.id === companyId);
  const establishments = selectedCompany?.establishments ?? [];
  const selectedEstablishment = establishments.find(
    (establishment) => establishment.id === establishmentId
  );

  useEffect(() => {
    setValue('companyId', '');
    setValue('establishmentId', '');
    setValue('warehouseId', '');
    setValue('terminalId', '');
  }, [scopeType, setValue]);

  useEffect(() => {
    setValue('establishmentId', '');
    setValue('warehouseId', '');
    setValue('terminalId', '');
  }, [companyId, setValue]);

  useEffect(() => {
    setValue('warehouseId', '');
    setValue('terminalId', '');
  }, [establishmentId, setValue]);

  return (
    <form
      id={formId}
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      {errorMessage ? (
        <p role="alert" className="text-danger-700 dark:text-danger-400 text-sm font-medium">
          {errorMessage}
        </p>
      ) : null}
      <FormField label="Rol" htmlFor="asignacion-role" error={errors.roleId?.message}>
        <select
          id="asignacion-role"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('roleId')}
        >
          <option value="">Selecciona un rol</option>
          {(rolesResult.data?.items ?? []).map((rol) => (
            <option key={rol.id} value={rol.id}>
              {rol.name}
            </option>
          ))}
        </select>
      </FormField>

      <FormField label="Tipo de ámbito" htmlFor="asignacion-scope" error={errors.scopeType?.message}>
        <select
          id="asignacion-scope"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('scopeType')}
        >
          {SCOPE_TYPES.map((type) => (
            <option key={type} value={type}>
              {type}
            </option>
          ))}
        </select>
      </FormField>

      {scopeType !== 'GLOBAL' ? (
        <FormField label="Empresa" htmlFor="asignacion-company" error={errors.companyId?.message}>
          <select
            id="asignacion-company"
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
            {...register('companyId')}
          >
            <option value="">Selecciona una empresa</option>
            {companies.map((company) => (
              <option key={company.id} value={company.id}>
                {company.tradeName ?? company.legalName}
              </option>
            ))}
          </select>
        </FormField>
      ) : null}

      {scopeType === 'ESTABLECIMIENTO' || scopeType === 'ALMACEN' || scopeType === 'TERMINAL' ? (
        <FormField
          label="Establecimiento"
          htmlFor="asignacion-establishment"
          error={errors.establishmentId?.message}
        >
          <select
            id="asignacion-establishment"
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
            {...register('establishmentId')}
          >
            <option value="">Selecciona un establecimiento</option>
            {establishments.map((establishment) => (
              <option key={establishment.id} value={establishment.id}>
                {establishment.name}
              </option>
            ))}
          </select>
        </FormField>
      ) : null}

      {scopeType === 'ALMACEN' ? (
        <FormField label="Almacén" htmlFor="asignacion-warehouse" error={errors.warehouseId?.message}>
          <select
            id="asignacion-warehouse"
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
            {...register('warehouseId')}
          >
            <option value="">Selecciona un almacén</option>
            {(selectedEstablishment?.warehouses ?? []).map((warehouse) => (
              <option key={warehouse.id} value={warehouse.id}>
                {warehouse.name}
              </option>
            ))}
          </select>
        </FormField>
      ) : null}

      {scopeType === 'TERMINAL' ? (
        <FormField label="Terminal" htmlFor="asignacion-terminal" error={errors.terminalId?.message}>
          <select
            id="asignacion-terminal"
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
            {...register('terminalId')}
          >
            <option value="">Selecciona un terminal</option>
            {(selectedEstablishment?.cashRegisters ?? []).map((register_) => (
              <option key={register_.id} value={register_.id}>
                {register_.name}
              </option>
            ))}
          </select>
        </FormField>
      ) : null}

      <FormField label="Vigente desde (opcional)" htmlFor="asignacion-valid-from">
        <input
          id="asignacion-valid-from"
          type="datetime-local"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('validFrom')}
        />
      </FormField>

      <FormField label="Vigente hasta (opcional)" htmlFor="asignacion-valid-until">
        <input
          id="asignacion-valid-until"
          type="datetime-local"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('validUntil')}
        />
      </FormField>

      {formId ? null : (
        <div className="flex justify-end gap-3">
          <Button type="button" variant="secondary" onClick={onCancel} disabled={isSubmitting}>
            Cancelar
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            Asignar rol
          </Button>
        </div>
      )}
    </form>
  );
}
