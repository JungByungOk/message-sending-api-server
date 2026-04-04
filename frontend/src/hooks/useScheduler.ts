import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createJob,
  deleteAllJobs,
  deleteJob,
  getJobs,
  pauseJob,
  resumeJob,
  stopJob,
} from '@/api/scheduler';
import type { JobControlRequest, ScheduleJobRequest } from '@/types/scheduler';

// 전체 작업 목록 조회 훅 (10초 자동 갱신)
export const useJobs = () => {
  return useQuery({
    queryKey: ['jobs'],
    queryFn: getJobs,
    refetchInterval: 10000,
  });
};

// 작업 생성 뮤테이션 훅
export const useCreateJob = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ScheduleJobRequest) => createJob(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['jobs'] });
    },
  });
};

// 작업 일시 정지 뮤테이션 훅
export const usePauseJob = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: JobControlRequest) => pauseJob(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['jobs'] });
    },
  });
};

// 작업 재개 뮤테이션 훅
export const useResumeJob = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: JobControlRequest) => resumeJob(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['jobs'] });
    },
  });
};

// 작업 중지 뮤테이션 훅
export const useStopJob = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: JobControlRequest) => stopJob(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['jobs'] });
    },
  });
};

// 작업 삭제 뮤테이션 훅
export const useDeleteJob = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: JobControlRequest) => deleteJob(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['jobs'] });
    },
  });
};

// 전체 작업 삭제 뮤테이션 훅
export const useDeleteAllJobs = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteAllJobs,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['jobs'] });
    },
  });
};
