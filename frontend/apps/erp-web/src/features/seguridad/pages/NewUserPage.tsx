import { useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router';
import { ApiError } from '@boticas/api-client';
import { Button, Card, PageHeader } from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import { crearUsuario, asignarRolUsuario } from '../api/usuarios.api';
import type { UsuarioFormValues } from '../schemas/usuario.schema';
import type { AsignacionRolFormValues } from '../schemas/asignacion-rol.schema';
import { UsuarioForm } from '../components/UsuarioForm';
import { AsignacionRolForm } from '../components/AsignacionRolForm';

const ROLE_FORM_ID = 'new-user-role-assignment-form';

type Phase = 'idle' | 'creating' | 'assigning' | 'retrying-assignment' | 'partial-failure';

export function NewUserPage() {
  const { tenantId } = useAuthSession();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [assignRoleEnabled, setAssignRoleEnabled] = useState(false);
  const [phase, setPhase] = useState<Phase>('idle');
  const [createError, setCreateError] = useState<string | null>(null);
  const [assignError, setAssignError] = useState<string | null>(null);
  const [createdUserId, setCreatedUserId] = useState<string | null>(null);
  const [pendingRoleValues, setPendingRoleValues] = useState<AsignacionRolFormValues | null>(null);
  const createdUserIdRef = useRef<string | null>(null);
  const pendingUserValuesRef = useRef<UsuarioFormValues | null>(null);

  const createMutation = useMutation({
    mutationFn: (values: UsuarioFormValues) =>
      crearUsuario(apiClient, {
        tenantId: tenantId ?? '',
        documentType: values.documentType,
        documentNumber: values.documentNumber,
        displayName: values.displayName || undefined,
        firstNames: values.firstNames || undefined,
        lastNames: values.lastNames || undefined,
        email: values.email,
        username: values.username || undefined,
        phone: values.phone || undefined,
        mfaRequired: values.mfaRequired
      })
  });

  const assignRoleMutation = useMutation({
    mutationFn: ({ userId, values }: { userId: string; values: AsignacionRolFormValues }) =>
      asignarRolUsuario(apiClient, userId, {
        tenantId: tenantId ?? '',
        roleId: values.roleId,
        scopeType: values.scopeType,
        companyId: values.companyId || undefined,
        establishmentId: values.establishmentId || undefined,
        warehouseId: values.warehouseId || undefined,
        terminalId: values.terminalId || undefined,
        validFrom: values.validFrom || undefined,
        validUntil: values.validUntil || undefined
      })
  });

  const goToDetail = (userId: string) => {
    void navigate(`/seguridad/usuarios/${userId}`);
  };

  const createUser = (values: UsuarioFormValues, onCreated: (userId: string) => void) => {
    setCreateError(null);
    setPhase('creating');
    createMutation.mutate(values, {
      onError: (error: unknown) => {
        setCreateError(error instanceof ApiError ? error.message : 'No se pudo crear el usuario.');
        setPhase('idle');
      },
      onSuccess: (usuario) => {
        createdUserIdRef.current = usuario.id;
        setCreatedUserId(usuario.id);
        void queryClient.invalidateQueries({ queryKey: ['seguridad', 'usuarios'] });
        onCreated(usuario.id);
      }
    });
  };

  const assignRole = (
    userId: string,
    values: AsignacionRolFormValues,
    inProgressPhase: 'assigning' | 'retrying-assignment' = 'assigning'
  ) => {
    if (inProgressPhase === 'assigning') setAssignError(null);
    setPendingRoleValues(values);
    setPhase(inProgressPhase);
    assignRoleMutation.mutate(
      { userId, values },
      {
        onError: (error: unknown) => {
          setAssignError(error instanceof ApiError ? error.message : 'No se pudo asignar el rol.');
          setPhase('partial-failure');
        },
        onSuccess: () => {
          goToDetail(userId);
        }
      }
    );
  };

  const handleCreateUser = (values: UsuarioFormValues) => {
    if (!assignRoleEnabled) {
      createUser(values, goToDetail);
      return;
    }
    pendingUserValuesRef.current = values;
    const roleForm = document.getElementById(ROLE_FORM_ID) as HTMLFormElement | null;
    roleForm?.requestSubmit();
  };

  const handleRoleFormValidated = (roleValues: AsignacionRolFormValues) => {
    const userValues = pendingUserValuesRef.current;
    if (!userValues) return;
    createUser(userValues, (userId) => assignRole(userId, roleValues));
  };

  // pendingRoleValues siempre está seteado aquí: este botón solo se renderiza en phase
  // 'partial-failure', que assignRole únicamente alcanza después de setPendingRoleValues.
  const handleRetryAssignment = () =>
    assignRole(createdUserIdRef.current!, pendingRoleValues!, 'retrying-assignment');

  const handleCancelRoleAssignment = () => setAssignRoleEnabled(false);

  const showPartialFailure =
    (phase === 'partial-failure' || phase === 'retrying-assignment') && createdUserId !== null;

  if (showPartialFailure) {
    return (
      <div className="mx-auto max-w-3xl">
        <PageHeader
          title="Nuevo usuario"
          context="Seguridad / Usuarios / Nuevo usuario"
          description="El usuario fue creado, pero la asignación de rol no se pudo completar."
        />
        <Card className="mt-6 p-5">
          <p className="text-danger-700 dark:text-danger-400 text-sm font-medium">
            Usuario creado, pero no se pudo asignar el rol: {assignError}
          </p>
          <div className="mt-4 flex items-center gap-3">
            <Button onClick={handleRetryAssignment} disabled={assignRoleMutation.isPending}>
              {assignRoleMutation.isPending ? 'Reintentando…' : 'Reintentar asignación'}
            </Button>
            <Link
              to={`/seguridad/usuarios/${createdUserId}`}
              className="text-primary-700 dark:text-primary-400 text-sm font-semibold"
            >
              Ir al detalle del usuario
            </Link>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader
        title="Nuevo usuario"
        context="Seguridad / Usuarios / Nuevo usuario"
        description="Crea una cuenta de acceso al sistema y, opcionalmente, asígnale un rol."
      />

      <Card className="mt-6 p-5">
        <h2 className="font-bold text-neutral-950 dark:text-white">Datos del usuario</h2>
        <div className="mt-4">
          <UsuarioForm
            onSubmit={handleCreateUser}
            submitLabel="Crear usuario"
            isSubmitting={phase === 'creating' || phase === 'assigning'}
          />
          {createError && (
            <p className="mt-3 text-sm text-red-600 dark:text-red-400" role="alert">
              {createError}
            </p>
          )}
        </div>
      </Card>

      <Card className="mt-6 p-5">
        <label className="flex w-fit cursor-pointer items-center gap-2.5 text-sm font-semibold text-neutral-700 dark:text-neutral-200">
          <input
            type="checkbox"
            className="text-primary-700 focus:ring-primary-600 size-4 rounded border-neutral-300 dark:border-neutral-600"
            checked={assignRoleEnabled}
            onChange={(event) => setAssignRoleEnabled(event.target.checked)}
          />
          Asignar rol ahora
        </label>
        {assignRoleEnabled && (
          <div className="mt-4">
            <AsignacionRolForm
              formId={ROLE_FORM_ID}
              tenantId={tenantId ?? ''}
              onSubmit={handleRoleFormValidated}
              onCancel={handleCancelRoleAssignment}
              isSubmitting={phase === 'assigning'}
            />
            <Button
              type="button"
              variant="secondary"
              className="mt-3"
              onClick={handleCancelRoleAssignment}
              disabled={phase === 'assigning'}
            >
              Quitar asignación de rol
            </Button>
          </div>
        )}
      </Card>
    </div>
  );
}
