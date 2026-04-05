import type { OnboardingResult, OnboardingStartRequest, OnboardingStatus, DkimRecordsDTO } from '@/types/onboarding';
import apiClient from './client';

// 온보딩 시작
export const startOnboarding = async (data: OnboardingStartRequest): Promise<OnboardingResult> => {
  const { data: result } = await apiClient.post<OnboardingResult>('/onboarding/start', data);
  return result;
};

// 온보딩 상태 조회
export const getOnboardingStatus = async (tenantId: string): Promise<OnboardingStatus> => {
  const { data } = await apiClient.get<OnboardingStatus>(`/onboarding/${tenantId}/status`);
  return data;
};

// DKIM 레코드 조회
export const getDkimRecords = async (tenantId: string): Promise<DkimRecordsDTO> => {
  const { data } = await apiClient.get<DkimRecordsDTO>(`/onboarding/${tenantId}/dkim`);
  return data;
};

// 테넌트 활성화
export const activateTenant = async (tenantId: string): Promise<void> => {
  await apiClient.post(`/onboarding/${tenantId}/activate`);
};
