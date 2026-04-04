export interface AwsSettings {
  endpoint: string;
  region: string;
  authType: 'API_KEY' | 'IAM';
  apiKey: string;
  accessKey: string;
  secretKey: string;
}

export interface AwsSettingsResponse {
  endpoint: string;
  region: string;
  authType: string;
  apiKeyMasked: string;
  accessKey: string;
  secretKeyMasked: string;
  configured: boolean;
  source: 'database' | 'none';
  updatedAt: string | null;
}

export interface AwsTestResult {
  connected: boolean;
  message: string;
  statusCode: number;
}
