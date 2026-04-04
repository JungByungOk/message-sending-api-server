CREATE TABLE IF NOT EXISTS SYSTEM_CONFIG (
    CONFIG_KEY   VARCHAR(100) PRIMARY KEY,
    CONFIG_VALUE TEXT,
    DESCRIPTION  VARCHAR(500),
    ENCRYPTED    BOOLEAN DEFAULT FALSE,
    UPDATED_AT   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- API Gateway 연동 설정
INSERT INTO SYSTEM_CONFIG (CONFIG_KEY, CONFIG_VALUE, DESCRIPTION, ENCRYPTED) VALUES
('gateway.endpoint', '', 'API Gateway Endpoint URL', FALSE),
('gateway.region', '', 'API Gateway AWS 리전 (IAM 서명용)', FALSE),
('gateway.auth-type', 'API_KEY', '인증 방식 (API_KEY 또는 IAM)', FALSE),
('gateway.api-key', '', 'API Gateway API Key', TRUE),
('gateway.access-key', '', 'IAM Access Key (IAM 인증 시)', TRUE),
('gateway.secret-key', '', 'IAM Secret Key (IAM 인증 시)', TRUE)
ON CONFLICT (CONFIG_KEY) DO NOTHING;
