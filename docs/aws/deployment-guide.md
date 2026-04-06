# Joins EMS — AWS 배포 가이드

## 사전 준비

### 1. AWS 계정 생성

AWS 계정이 없다면 먼저 생성합니다.

1. https://aws.amazon.com/ 접속 → **"AWS 계정 생성"** 클릭
2. 이메일, 비밀번호, 계정 이름 입력
3. 결제 정보 (신용카드) 등록 — Free Tier 범위 내 사용 시 과금 없음
4. 본인 인증 (휴대폰 SMS/음성)
5. Support 플랜 선택 → **"Basic Support - 무료"** 선택
6. 계정 생성 완료 → AWS Console 로그인

### 2. IAM 사용자 생성 (Access Key 발급)

> **루트 계정으로 직접 작업하지 마세요.** IAM 사용자를 생성하여 사용합니다.

1. AWS Console → **IAM** 서비스 이동
2. 좌측 메뉴 **"사용자"** → **"사용자 생성"**
3. 사용자 이름: `ems-deploy` (원하는 이름)
4. **"다음"** → 권한 설정:
   - **"직접 정책 연결"** 선택
   - `AdministratorAccess` 검색 후 체크 (CDK 배포에 필요)
   - > 운영 환경에서는 최소 권한 원칙에 따라 필요한 권한만 부여하세요
5. **"다음"** → **"사용자 생성"**
6. 생성된 사용자 클릭 → **"보안 자격 증명"** 탭
7. **"액세스 키 만들기"** → **"Command Line Interface(CLI)"** 선택
8. **Access Key ID**와 **Secret Access Key**를 안전하게 저장
   - > Secret Access Key는 이 화면에서만 확인 가능합니다. 반드시 저장하세요.

### 3. AWS CLI 설치 및 설정

**AWS CLI 설치:**
```bash
# Windows (MSI 설치)
# https://awscli.amazonaws.com/AWSCLIV2.msi 다운로드 후 실행

# Mac
brew install awscli

# Linux
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip && sudo ./aws/install
```

설치 확인:
```bash
aws --version
# aws-cli/2.x.x ...
```

**AWS CLI 설정** (Step 2에서 발급받은 Access Key 입력):
```bash
aws configure
```
```
AWS Access Key ID [None]: AKIA...          ← Step 2에서 발급받은 Access Key ID
AWS Secret Access Key [None]: wJalr...     ← Step 2에서 발급받은 Secret Access Key
Default region name [None]: ap-northeast-2 ← 서울 리전
Default output format [None]: json
```

설정 확인:
```bash
aws sts get-caller-identity
# 계정 ID, IAM 사용자 정보가 출력되면 성공
```

### 4. Node.js 18+ 설치

```bash
# 설치 확인
node --version
# v18.x.x 이상 필요

# 미설치 시: https://nodejs.org/ 에서 LTS 버전 다운로드
```

### 5. AWS CDK CLI 설치

```bash
npm install -g aws-cdk
cdk --version
# 2.x.x 출력 확인
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
# 기본 (IP 제한 없음, 테스트용)
cdk deploy

# IP Whitelist — Context 방식
cdk deploy --context esmServerIp=203.0.113.10/32

# IP Whitelist — CfnParameter 방식
cdk deploy --parameters AllowedIps="203.0.113.10/32,198.51.100.5/32"
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

### Step 7: Callback Secret 보안 강화 (권장)
배포 시 SSM에 `REPLACE_ME` 플레이스홀더가 설정됩니다. 실제 시크릿으로 교체하세요:
```bash
aws ssm put-parameter \
  --name /ems/callback_secret \
  --value "실제시크릿값" \
  --type SecureString \
  --overwrite
```

### Step 8: SES Production 전환 (운영 시)
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
