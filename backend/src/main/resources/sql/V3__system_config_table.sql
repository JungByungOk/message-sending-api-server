CREATE TABLE IF NOT EXISTS SYSTEM_CONFIG (
    CONFIG_KEY   VARCHAR(100) PRIMARY KEY,
    CONFIG_VALUE TEXT,
    DESCRIPTION  VARCHAR(500),
    ENCRYPTED    BOOLEAN DEFAULT FALSE,
    UPDATED_AT   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- API Gateway 설정
INSERT INTO SYSTEM_CONFIG (CONFIG_KEY, CONFIG_VALUE, DESCRIPTION, ENCRYPTED) VALUES
('gateway.endpoint', '', 'API Gateway Base URL', FALSE),
('gateway.region', 'ap-northeast-2', 'API Gateway AWS 리전', FALSE),
('gateway.auth-type', 'API_KEY', '인증 방식 (API_KEY 또는 IAM)', FALSE),
('gateway.api-key', '', 'API Gateway API Key', TRUE),
('gateway.access-key', '', 'IAM Access Key', TRUE),
('gateway.secret-key', '', 'IAM Secret Key', TRUE),
('gateway.send-path', '/send-email', '이메일 발송 경로', FALSE),
('gateway.results-path', '/results', '발송 결과 조회 경로', FALSE),
('gateway.config-path', '/config', 'SSM 설정 동기화 경로', FALSE)
ON CONFLICT (CONFIG_KEY) DO NOTHING;

-- Callback 설정
INSERT INTO SYSTEM_CONFIG (CONFIG_KEY, CONFIG_VALUE, DESCRIPTION, ENCRYPTED) VALUES
('callback.url', '', 'Callback URL (Lambda → ESM)', FALSE),
('callback.secret', '', 'Callback Secret (요청 무결성 검증)', TRUE)
ON CONFLICT (CONFIG_KEY) DO NOTHING;

-- 수신 모드 설정
INSERT INTO SYSTEM_CONFIG (CONFIG_KEY, CONFIG_VALUE, DESCRIPTION, ENCRYPTED) VALUES
('delivery.mode', 'callback', '수신 모드 (callback 또는 polling)', FALSE),
('delivery.polling-interval', '300000', '보정 폴링 주기 (ms, 기본 5분)', FALSE)
ON CONFLICT (CONFIG_KEY) DO NOTHING;
