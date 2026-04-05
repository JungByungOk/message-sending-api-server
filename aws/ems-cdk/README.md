# Joins EMS - AWS CDK Infrastructure

AWS CDK로 EMS 이메일 발송 인프라를 자동 구축합니다.

## 생성되는 리소스

| 리소스 | 이름 | 용도 |
|--------|------|------|
| API Gateway | ems-api | ESM → AWS 연동 엔드포인트 |
| Lambda | ems-email-sender | SQS → SES 이메일 발송 |
| Lambda | ems-event-processor | SNS → DynamoDB 저장 + ESM 콜백 |
| Lambda | ems-event-query | DynamoDB 발송결과 조회 |
| Lambda | ems-tenant-setup | 테넌트/Identity/ConfigSet/템플릿 관리 |
| Lambda | ems-config-updater | SSM Parameter Store 설정 업데이트 |
| SQS | ems-send-queue (+DLQ) | 비동기 발송 큐 |
| SNS | ems-ses-events | SES 이벤트 수신 |
| DynamoDB | ems-send-results | 발송결과 (TTL 7일) |
| DynamoDB | ems-tenant-config | 테넌트 설정 (TTL 1시간) |
| DynamoDB | ems-idempotency | 중복발송 방지 (TTL 24시간) |
| SSM | /ems/mode, callback_url, callback_secret | 수신 모드 설정 |

## 사전 준비

```bash
# Node.js 18+ 필요
node --version

# AWS CLI 설정
aws configure

# AWS CDK CLI 설치
npm install -g aws-cdk
```

## 배포

```bash
cd aws/ems-cdk

# 의존성 설치
npm install

# CDK 부트스트랩 (최초 1회)
cdk bootstrap

# 변경사항 미리보기
cdk diff

# 배포
cdk deploy
```

## 배포 후 출력값

배포 완료 시 아래 값이 출력됩니다. ESM 설정 화면에 입력하세요.

| 출력 | ESM 설정 항목 |
|------|---------------|
| `ApiGatewayUrl` | Gateway Endpoint URL |
| `ApiKeyId` | AWS 콘솔에서 API Key 값 확인 후 입력 |

## 삭제

```bash
cdk destroy
```

## API Gateway 엔드포인트

| Method | 경로 | 용도 |
|--------|------|------|
| POST | /send-email | 이메일 발송 (→ SQS) |
| GET | /results | 발송결과 조회 |
| PUT | /config | 수신 모드/콜백 설정 (→ SSM) |
| GET/POST/DELETE | /tenant-setup | 테넌트/Identity/ConfigSet/템플릿 관리 |
