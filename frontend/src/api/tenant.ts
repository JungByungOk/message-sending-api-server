import type { CreateTenantRequest, Tenant, TenantListResponse, TenantSender, UpdateTenantRequest } from '@/types/tenant';
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

// 발신자 목록 조회
export const getSenders = async (tenantId: string): Promise<TenantSender[]> => {
  const { data } = await apiClient.get<TenantSender[]>(`/tenant/${tenantId}/senders`);
  return data;
};

// 발신자 등록
export const addSender = async (tenantId: string, sender: { email: string; displayName?: string; isDefault?: boolean }): Promise<TenantSender> => {
  const { data } = await apiClient.post<TenantSender>(`/tenant/${tenantId}/senders`, sender);
  return data;
};

// 발신자 삭제
export const removeSender = async (tenantId: string, email: string): Promise<void> => {
  await apiClient.delete(`/tenant/${tenantId}/senders/${encodeURIComponent(email)}`);
};

// 테넌트 일시정지
export const pauseTenant = async (id: string): Promise<void> => {
  await apiClient.post(`/tenant/${id}/pause`);
};

// 테넌트 발송 재개
export const resumeTenant = async (id: string): Promise<void> => {
  await apiClient.post(`/tenant/${id}/resume`);
};
