CREATE TABLE IF NOT EXISTS TENANT_REGISTRY
(
    tenant_id           VARCHAR(36)  NOT NULL COMMENT 'UUID 테넌트 식별자',
    tenant_name         VARCHAR(100) NOT NULL COMMENT '테넌트명',
    domain              VARCHAR(255) NOT NULL COMMENT '도메인',
    api_key             VARCHAR(64)  NOT NULL COMMENT 'API 키 (nte_ 접두사 포함)',
    config_set_name     VARCHAR(100)          DEFAULT NULL COMMENT 'SES ConfigSet 명',
    verification_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '도메인 인증 상태: PENDING / VERIFIED / FAILED',
    quota_daily         INT          NOT NULL DEFAULT 10000 COMMENT '일일 발송 할당량',
    quota_monthly       INT          NOT NULL DEFAULT 300000 COMMENT '월별 발송 할당량',
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '테넌트 상태: ACTIVE / INACTIVE / SUSPENDED',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (tenant_id),
    UNIQUE KEY uq_tenant_domain (domain),
    UNIQUE KEY uq_tenant_api_key (api_key),
    INDEX idx_tenant_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '테넌트 레지스트리';
