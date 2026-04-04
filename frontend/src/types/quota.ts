export interface QuotaDetail {
  limit: number;
  used: number;
  remaining: number;
}

export interface QuotaInfo {
  tenantId: string;
  daily: QuotaDetail;
  monthly: QuotaDetail;
}
