export interface AwsSettings {
  // API Gateway
  gatewayEndpoint: string;
  gatewayRegion: string;
  gatewayApiKey: string;
  gatewaySendPath: string;
  gatewayResultsPath: string;
  gatewayConfigPath: string;
  gatewayTenantSetupPath: string;
  // Callback
  callbackUrl: string;
  callbackSecret: string;
  // Delivery
  deliveryMode: 'callback' | 'polling';
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
  // Callback
  callbackUrl: string;
  callbackSecretMasked: string;
  callbackConfigured: boolean;
  // Delivery
  deliveryMode: string;
  pollingInterval: string;
  // Meta
  updatedAt: string | null;
}

export interface AwsTestResult {
  connected: boolean;
  message: string;
  statusCode: number;
}
