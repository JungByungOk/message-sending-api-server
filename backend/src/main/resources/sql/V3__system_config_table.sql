CREATE TABLE IF NOT EXISTS SYSTEM_CONFIG (
    CONFIG_KEY   VARCHAR(100) PRIMARY KEY,
    CONFIG_VALUE TEXT,
    DESCRIPTION  VARCHAR(500),
    ENCRYPTED    BOOLEAN DEFAULT FALSE,
    UPDATED_AT   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- AWS SES 기본 설정
INSERT INTO SYSTEM_CONFIG (CONFIG_KEY, CONFIG_VALUE, DESCRIPTION, ENCRYPTED) VALUES
('aws.ses.region', '', 'AWS SES 리전', FALSE),
('aws.ses.access-key', '', 'AWS SES Access Key', TRUE),
('aws.ses.secret-key', '', 'AWS SES Secret Key', TRUE),
('aws.endpoint', '', 'AWS Endpoint Override (LocalStack 등)', FALSE),
('aws.dynamo.region', '', 'AWS DynamoDB 리전', FALSE),
('aws.dynamo.access-key', '', 'AWS DynamoDB Access Key', TRUE),
('aws.dynamo.secret-key', '', 'AWS DynamoDB Secret Key', TRUE)
ON CONFLICT (CONFIG_KEY) DO NOTHING;
