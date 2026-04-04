import type { SuppressionListResponse } from '@/types/suppression';
import type { PageParams } from '@/types/api';
import apiClient from './client';

// 수신 거부 목록 조회
export const getSuppressions = async (
  tenantId: string,
  params?: PageParams,
): Promise<SuppressionListResponse> => {
  const { data } = await apiClient.get<SuppressionListResponse>(
    `/suppression/tenant/${tenantId}`,
    { params },
  );
  return data;
};

// 수신 거부 삭제
export const removeSuppression = async (tenantId: string, email: string): Promise<void> => {
  await apiClient.delete(`/suppression/tenant/${tenantId}/${encodeURIComponent(email)}`);
};
