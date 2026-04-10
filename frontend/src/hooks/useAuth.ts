import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { authApi } from '@/api/auth';
import type { CreateUserRequest, UpdateUserRequest, ChangePasswordRequest } from '@/types/auth';

export function useMe() {
  return useQuery({
    queryKey: ['auth', 'me'],
    queryFn: () => authApi.getMe().then((res) => res.data),
  });
}

export function useUsers() {
  return useQuery({
    queryKey: ['users'],
    queryFn: () => authApi.getUsers().then((res) => res.data),
  });
}

export function useCreateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateUserRequest) => authApi.createUser(data).then((res) => res.data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users'] }),
  });
}

export function useUpdateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, data }: { userId: number; data: UpdateUserRequest }) =>
      authApi.updateUser(userId, data).then((res) => res.data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users'] }),
  });
}

export function useDeleteUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: number) => authApi.deleteUser(userId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users'] }),
  });
}

export function useChangePassword() {
  return useMutation({
    mutationFn: (data: ChangePasswordRequest) => authApi.changePassword(data),
  });
}
