export interface AwsSettings {
  // API Gateway
  gatewayEndpoint: string;
  gatewayRegion: string;
  gatewayApiKey: string;
  gatewaySendPath: string;
  gatewayResultsPath: string;
  gatewayConfigPath: string;
  gatewayTenantSetupPath: string;
  // Callback (legacy, not displayed in UI)
  callbackUrl?: string;
  callbackSecret?: string;
  // Delivery
  deliveryMode?: string;
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
  // Callback (legacy)
  callbackUrl?: string;
  callbackSecretMasked?: string;
  callbackConfigured?: boolean;
  // Delivery
  deliveryMode?: string;
  pollingInterval: string;
  // Meta
  updatedAt: string | null;
}

export interface AwsTestResult {
  connected: boolean;
  message: string;
  statusCode: number;
}
