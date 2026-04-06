import type { OnboardingResult, OnboardingStartRequest, OnboardingStatus, DkimRecordsDTO, EmailVerificationStatus } from '@/types/onboarding';
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

// 이메일 인증 요청
export const verifyEmail = async (tenantId: string, email: string): Promise<void> => {
  await apiClient.post(`/onboarding/${tenantId}/verify-email`, { email });
};

// 이메일 인증 상태 조회
export const getEmailVerificationStatus = async (
  tenantId: string,
  email: string,
): Promise<EmailVerificationStatus> => {
  const { data } = await apiClient.get<EmailVerificationStatus>(
    `/onboarding/${tenantId}/email-status/${email}`,
  );
  return data;
};

// 인증 이메일 재발송
export const resendVerification = async (tenantId: string, email: string): Promise<void> => {
  await apiClient.post(`/onboarding/${tenantId}/resend-verification/${email}`);
};
