import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  activateTenant,
  addSender,
  createTenant,
  deactivateTenant,
  deleteTenantPermanently,
  getSenders,
  getTenant,
  getTenants,
  pauseTenant,
  regenerateApiKey,
  removeSender,
  resumeTenant,
  updateTenant,
} from '@/api/tenant';
import type { CreateTenantRequest, UpdateTenantRequest } from '@/types/tenant';
import type { PageParams } from '@/types/api';

// 테넌트 목록 조회 훅
export const useTenants = (params?: PageParams) => {
  return useQuery({
    queryKey: ['tenants', params],
    queryFn: () => getTenants(params),
  });
};

// 테넌트 단건 조회 훅
export const useTenant = (id: string) => {
  return useQuery({
    queryKey: ['tenant', id],
    queryFn: () => getTenant(id),
    enabled: !!id,
  });
};

// 테넌트 생성 뮤테이션 훅
export const useCreateTenant = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateTenantRequest) => createTenant(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['tenants'] });
    },
  });
};

// 테넌트 수정 뮤테이션 훅
export const useUpdateTenant = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateTenantRequest }) =>
      updateTenant(id, payload),
    onSuccess: (_data, variables) => {
      void queryClient.invalidateQueries({ queryKey: ['tenants'] });
      void queryClient.invalidateQueries({ queryKey: ['tenant', variables.id] });
    },
  });
};

// 테넌트 비활성화 뮤테이션 훅
export const useDeactivateTenant = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => deactivateTenant(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['tenants'] });
    },
  });
};

// 테넌트 활성화 뮤테이션 훅
export const useActivateTenant = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => activateTenant(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['tenants'] });
    },
  });
};

// 테넌트 영구 삭제 뮤테이션 훅
export const useDeleteTenant = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => deleteTenantPermanently(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['tenants'] });
    },
  });
};

// API 키 재생성 뮤테이션 훅
export const useRegenerateApiKey = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => regenerateApiKey(id),
    onSuccess: (_data, id) => {
      void queryClient.invalidateQueries({ queryKey: ['tenant', id] });
    },
  });
};

// 발신자 목록 조회 훅
export const useSenders = (tenantId: string) => {
  return useQuery({
    queryKey: ['senders', tenantId],
    queryFn: () => getSenders(tenantId),
    enabled: !!tenantId,
  });
};

// 발신자 등록 뮤테이션 훅
export const useAddSender = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ tenantId, sender }: { tenantId: string; sender: { email: string; displayName?: string; isDefault?: boolean } }) =>
      addSender(tenantId, sender),
    onSuccess: (_data, variables) => {
      void queryClient.invalidateQueries({ queryKey: ['senders', variables.tenantId] });
    },
  });
};

// 발신자 삭제 뮤테이션 훅
export const useRemoveSender = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ tenantId, email }: { tenantId: string; email: string }) =>
      removeSender(tenantId, email),
    onSuccess: (_data, variables) => {
      void queryClient.invalidateQueries({ queryKey: ['senders', variables.tenantId] });
    },
  });
};

// 테넌트 일시정지 뮤테이션 훅
export const usePauseTenant = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => pauseTenant(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['tenants'] });
    },
  });
};

// 테넌트 발송 재개 뮤테이션 훅
export const useResumeTenant = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => resumeTenant(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['tenants'] });
    },
  });
};
