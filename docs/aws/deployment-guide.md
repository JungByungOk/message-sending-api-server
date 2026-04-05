# Joins EMS — AWS 배포 가이드

## 사전 준비

### 1. AWS CLI 설정
```bash
aws configure
# AWS Access Key ID: [입력]
# AWS Secret Access Key: [입력]
# Default region name: ap-northeast-2
# Default output format: json
```

### 2. AWS CDK CLI 설치
```bash
npm install -g aws-cdk
cdk --version
```

### 3. Node.js 18+ 확인
```bash
node --version
```

## 배포 절차

### Step 1: 의존성 설치
```bash
cd aws/ems-cdk
npm install
```

### Step 2: CDK 부트스트랩 (최초 1회)
```bash
cdk bootstrap
```
CDK가 사용하는 S3 버킷과 IAM 역할을 AWS 계정에 생성합니다.

### Step 3: 변경사항 미리보기
```bash
cdk diff
```
생성/변경될 리소스를 확인합니다.

### Step 4: 배포
```bash
cdk deploy
```

배포 완료 시 출력:
```
Outputs:
EmsStack.ApiGatewayUrl = https://xxxxxxxxxx.execute-api.ap-northeast-2.amazonaws.com/prod/
EmsStack.ApiKeyId = xxxxxxxx
EmsStack.SesEventTopicArn = arn:aws:sns:ap-northeast-2:123456789:ems-ses-events
EmsStack.SendQueueUrl = https://sqs.ap-northeast-2.amazonaws.com/123456789/ems-send-queue
```

### Step 5: API Key 값 확인
API Key ID로 실제 값을 조회합니다:
```bash
aws apigateway get-api-key --api-key [ApiKeyId] --include-value --query "value" --output text
```

### Step 6: ESM 설정
ESM 관리 화면(설정 페이지)에서 입력:

| 설정 항목 | 값 |
|-----------|-----|
| Gateway Endpoint | `ApiGatewayUrl` 출력값 |
| API Key | Step 5에서 확인한 값 |
| Region | ap-northeast-2 |
| 발송 경로 | /send-email |
| 조회 경로 | /results |
| 설정 경로 | /config |
| 온보딩 경로 | /tenant-setup |
| Callback URL | https://[ESM서버]/ses/callback/event |
| Callback Secret | 임의 문자열 생성 |
| 수신 모드 | callback |

### Step 7: SES Production 전환 (운영 시)
SES는 기본 Sandbox 모드입니다. 운영 발송을 위해 Production 전환 요청:
```
AWS Console → SES → Account dashboard → Request production access
```

## IP Whitelist 설정

API Gateway에 ESM 서버 IP만 허용하려면 리소스 정책을 추가합니다:

```bash
aws apigateway update-rest-api \
  --rest-api-id [API_ID] \
  --patch-operations op=replace,path=/policy,value='{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":"*","Action":"execute-api:Invoke","Resource":"arn:aws:execute-api:ap-northeast-2:[ACCOUNT]:*","Condition":{"IpAddress":{"aws:SourceIp":["ESM_SERVER_IP/32"]}}}]}'
```

## 업데이트

Lambda 코드 변경 시:
```bash
cd aws/ems-cdk
cdk deploy
```

## 전체 삭제

```bash
cdk destroy
```

> **주의**: DynamoDB `ems-send-results`와 `ems-tenant-config`는 `RETAIN` 정책으로 설정되어 있어 스택 삭제 시 자동 삭제되지 않습니다. 수동 삭제 필요:
```bash
aws dynamodb delete-table --table-name ems-send-results
aws dynamodb delete-table --table-name ems-tenant-config
```

## 문제 해결

### Lambda 로그 확인
```bash
aws logs tail /aws/lambda/ems-email-sender --follow
aws logs tail /aws/lambda/ems-event-processor --follow
aws logs tail /aws/lambda/ems-tenant-setup --follow
```

### DLQ 메시지 확인
```bash
# 발송 DLQ
aws sqs receive-message --queue-url https://sqs.ap-northeast-2.amazonaws.com/[ACCOUNT]/ems-send-dlq

# 이벤트 DLQ
aws sqs receive-message --queue-url https://sqs.ap-northeast-2.amazonaws.com/[ACCOUNT]/ems-event-processor-dlq
```

### API Gateway 테스트
```bash
# 발송 테스트
curl -X POST https://[API_URL]/send-email \
  -H "x-api-key: [API_KEY]" \
  -H "Content-Type: application/json" \
  -d '{"from":"test@example.com","to":["success@simulator.amazonses.com"],"subject":"Test","body":"<p>Hello</p>"}'

# 결과 조회
curl "https://[API_URL]/results?tenant_id=test&limit=10" \
  -H "x-api-key: [API_KEY]"
```
