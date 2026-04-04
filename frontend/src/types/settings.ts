export interface AwsSettings {
  sesRegion: string;
  sesAccessKey: string;
  sesSecretKey: string;
  dynamoRegion: string;
  dynamoAccessKey: string;
  dynamoSecretKey: string;
  endpoint: string;
}

export interface AwsSettingsResponse {
  sesRegion: string;
  sesAccessKey: string;
  sesSecretKeyMasked: string;
  sesConfigured: boolean;
  dynamoRegion: string;
  dynamoAccessKey: string;
  dynamoSecretKeyMasked: string;
  dynamoConfigured: boolean;
  endpoint: string;
  source: 'database' | 'environment';
  updatedAt: string | null;
}

export interface AwsTestResult {
  sesConnected: boolean;
  sesMessage: string;
  dynamoConnected: boolean;
  dynamoMessage: string;
}
