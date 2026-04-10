import apiClient from './client';
import type { AwsSettings, AwsSettingsResponse, AwsTestResult } from '@/types/settings';

export const settingsApi = {
  getAwsSettings: () =>
    apiClient.get<AwsSettingsResponse>('/settings/aws').then((r) => r.data),

  saveAwsSettings: (data: AwsSettings) =>
    apiClient.put<AwsSettingsResponse>('/settings/aws', data).then((r) => r.data),

  testAwsConnection: (data: AwsSettings) =>
    apiClient.post<AwsTestResult>('/settings/aws/test', data).then((r) => r.data),

  getVdmStatus: () =>
    apiClient.get<{ enabled: boolean }>('/settings/vdm').then((r) => r.data),

  updateVdm: (enabled: boolean) =>
    apiClient.put<{ enabled: boolean }>('/settings/vdm', { enabled }).then((r) => r.data),

  getPollingInterval: () =>
    apiClient.get<{ intervalMinutes: number; intervalMs: number }>('/settings/polling-interval').then((r) => r.data),

  updatePollingInterval: (intervalMinutes: number) =>
    apiClient.put<{ intervalMinutes: number; intervalMs: number }>('/settings/polling-interval', { intervalMinutes }).then((r) => r.data),
};
