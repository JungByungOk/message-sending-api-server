export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: UserInfo;
}

export interface UserInfo {
  userId: number;
  username: string;
  displayName: string;
  role: string;
}

export interface TokenResponse {
  accessToken: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface CreateUserRequest {
  username: string;
  password: string;
  displayName: string;
  role: string;
}

export interface UpdateUserRequest {
  displayName?: string;
  role?: string;
  isActive?: boolean;
}
