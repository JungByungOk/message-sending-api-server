import apiClient from './client';
import type { AwsSettings, AwsSettingsResponse, AwsTestResult } from '@/types/settings';

export const settingsApi = {
  getAwsSettings: () =>
    apiClient.get<AwsSettingsResponse>('/settings/aws').then((r) => r.data),

  saveAwsSettings: (data: AwsSettings) =>
    apiClient.put<AwsSettingsResponse>('/settings/aws', data).then((r) => r.data),

  testAwsConnection: (data: AwsSettings) =>
    apiClient.post<AwsTestResult>('/settings/aws/test', data).then((r) => r.data),
};
