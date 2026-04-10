export interface AwsSettings {
  // API Gateway
  gatewayEndpoint: string;
  gatewayRegion: string;
  gatewayApiKey: string;
  gatewaySendPath: string;
  gatewayResultsPath: string;
  gatewayConfigPath: string;
  gatewayTenantSetupPath: string;
  // Delivery
  pollingInterval: string;
}

export interface AwsSettingsResponse {
  // API Gateway
  gatewayEndpoint: string;
  gatewayRegion: string;
  gatewayApiKeyMasked: string;
  gatewaySendPath: string;
  gatewayResultsPath: string;
  gatewayConfigPath: string;
  gatewayTenantSetupPath: string;
  gatewayConfigured: boolean;
  // Delivery
  pollingInterval: string;
  // Meta
  updatedAt: string | null;
}

export interface AwsTestResult {
  connected: boolean;
  message: string;
  statusCode: number;
}
