import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { settingsApi } from '@/api/settings';
import type { AwsSettings } from '@/types/settings';

export function useAwsSettings() {
  return useQuery({
    queryKey: ['settings', 'aws'],
    queryFn: settingsApi.getAwsSettings,
  });
}

export function useSaveAwsSettings() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: AwsSettings) => settingsApi.saveAwsSettings(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['settings', 'aws'] });
    },
  });
}

export function useTestAwsConnection() {
  return useMutation({
    mutationFn: (data: AwsSettings) => settingsApi.testAwsConnection(data),
  });
}
