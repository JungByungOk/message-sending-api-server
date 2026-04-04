export interface SuppressionEntry {
  id: number;
  tenantId: string;
  email: string;
  reason: 'BOUNCE' | 'COMPLAINT';
  createdAt: string;
}

export interface SuppressionListResponse {
  totalCount: number;
  suppressions: SuppressionEntry[];
}
