import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  activateTenant,
  createTenant,
  deactivateTenant,
  deleteTenantPermanently,
  getTenant,
  getTenants,
  regenerateApiKey,
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
