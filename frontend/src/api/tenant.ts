import type { CreateTenantRequest, Tenant, TenantListResponse, UpdateTenantRequest } from '@/types/tenant';
import type { PageParams } from '@/types/api';
import apiClient from './client';

// 테넌트 목록 조회
export const getTenants = async (params?: PageParams): Promise<TenantListResponse> => {
  const { data } = await apiClient.get<TenantListResponse>('/tenant/list', { params });
  return data;
};

// 테넌트 단건 조회
export const getTenant = async (id: string): Promise<Tenant> => {
  const { data } = await apiClient.get<Tenant>(`/tenant/${id}`);
  return data;
};

// 테넌트 생성
export const createTenant = async (payload: CreateTenantRequest): Promise<Tenant> => {
  const { data } = await apiClient.post<Tenant>('/tenant', payload);
  return data;
};

// 테넌트 수정
export const updateTenant = async (id: string, payload: UpdateTenantRequest): Promise<Tenant> => {
  const { data } = await apiClient.patch<Tenant>(`/tenant/${id}`, payload);
  return data;
};

// 테넌트 비활성화
export const deactivateTenant = async (id: string): Promise<void> => {
  await apiClient.delete(`/tenant/${id}`);
};

// 테넌트 활성화
export const activateTenant = async (id: string): Promise<void> => {
  await apiClient.post(`/tenant/${id}/activate`);
};

// API 키 재생성
export const regenerateApiKey = async (id: string): Promise<Tenant> => {
  const { data } = await apiClient.post<Tenant>(`/tenant/${id}/regenerate-key`);
  return data;
};

// 테넌트 영구 삭제
export const deleteTenantPermanently = async (id: string): Promise<void> => {
  await apiClient.delete(`/tenant/${id}/permanent`);
};
