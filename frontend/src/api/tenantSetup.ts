import type { TenantSetupResult, TenantSetupStartRequest, TenantSetupStatus, DkimRecordsDTO, EmailVerificationStatus } from '@/types/tenantSetup';
import apiClient from './client';

// 테넌트 초기 설정 시작
export const startSetup = async (data: TenantSetupStartRequest): Promise<TenantSetupResult> => {
  const { data: result } = await apiClient.post<TenantSetupResult>('/tenant-setup/start', data);
  return result;
};

// 설정 상태 조회
export const getSetupStatus = async (tenantId: string): Promise<TenantSetupStatus> => {
  const { data } = await apiClient.get<TenantSetupStatus>(`/tenant-setup/${tenantId}/status`);
  return data;
};

// DKIM 레코드 조회
export const getDkimRecords = async (tenantId: string): Promise<DkimRecordsDTO> => {
  const { data } = await apiClient.get<DkimRecordsDTO>(`/tenant-setup/${tenantId}/dkim`);
  return data;
};

// 테넌트 활성화
export const activateTenant = async (tenantId: string): Promise<void> => {
  await apiClient.post(`/tenant-setup/${tenantId}/activate`);
};

// 이메일 인증 요청
export const verifyEmail = async (tenantId: string, email: string): Promise<void> => {
  await apiClient.post(`/tenant-setup/${tenantId}/verify-email`, { email });
};

// 이메일 인증 상태 조회
export const getEmailVerificationStatus = async (
  tenantId: string,
  email: string,
): Promise<EmailVerificationStatus> => {
  const { data } = await apiClient.get<EmailVerificationStatus>(
    `/tenant-setup/${tenantId}/email-status/${email}`,
  );
  return data;
};

// 인증 이메일 재발송
export const resendVerification = async (tenantId: string, email: string): Promise<void> => {
  await apiClient.post(`/tenant-setup/${tenantId}/resend-verification/${email}`);
};
