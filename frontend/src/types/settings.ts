export interface AwsSettings {
  // API Gateway
  gatewayEndpoint: string;
  gatewayRegion: string;
  gatewayAuthType: 'API_KEY' | 'IAM';
  gatewayApiKey: string;
  gatewayAccessKey: string;
  gatewaySecretKey: string;
  gatewaySendPath: string;
  gatewayResultsPath: string;
  gatewayConfigPath: string;
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
  gatewayAuthType: string;
  gatewayApiKeyMasked: string;
  gatewayAccessKey: string;
  gatewaySecretKeyMasked: string;
  gatewaySendPath: string;
  gatewayResultsPath: string;
  gatewayConfigPath: string;
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
