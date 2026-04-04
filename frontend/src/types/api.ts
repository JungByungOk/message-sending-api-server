export interface ApiError {
  status: number;
  error: string;
  message: string;
}

export interface PageParams {
  page?: number;
  size?: number;
}
