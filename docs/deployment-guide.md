# 배포 가이드

> SES Native Migration (Phase 1~4) 배포 절차

## 사전 준비

| 항목 | 설명 |
|------|------|
| AWS CLI | 설치 및 설정 (`aws configure`) |
| AWS CDK | `npm install -g aws-cdk` (v2) |
| Node.js | 20.x 이상 |
| Java | 17 |
| Docker | PostgreSQL 로컬 실행용 |
| 리전 | ap-northeast-2 (서울) |

---

## 배포 순서

### Step 1: 로컬 DB 준비 (개발 환경)

```bash
# Docker Compose로 PostgreSQL 시작
cd message-sending-api-server
docker compose up -d

# DB 스키마 초기화 (최초 1회, 또는 볼륨 재생성 시)
docker exec -i ems-postgres psql -U ems -d ems < backend/src/main/resources/sql/V1__init_tables.sql
```

초기 관리자 계정: `admin` / `admin`

---

### Step 2: CDK v2 배포 (AWS 인프라)

```bash
cd aws/ems-cdk-v2

# 의존성 설치
npm install

# CDK 부트스트랩 (최초 1회)
cdk bootstrap --region ap-northeast-2

# 배포 (ESM 서버 IP 지정)
cdk deploy --context esmServerIp="서버공인IP/32"

# 또는 환경변수로
ESM_SERVER_IP="서버공인IP/32" cdk deploy
```

**배포 후 확인:**
- API Gateway URL → `backend/.env`의 Gateway 설정에 반영
- API Key → AWS 콘솔에서 값 확인 후 Settings 페이지에 입력

**배포 산출물:**

| 리소스 | 이름 |
|--------|------|
| API Gateway | ems-api-v2 |
| EventBridge | ems-ses-events |
| SQS | ems-send-queue + ems-send-dlq |
| Lambda | ems-email-sender, ems-enqueue, ems-event-processor, ems-suppression, ems-tenant-setup, ems-tenant-sync, ems-event-query |
| DynamoDB | ems-send-results, ems-tenant-config, ems-suppression |
| S3 | ems-batch-{account}-{region} |

---

### Step 3: Backend 배포

```bash
cd backend

# 환경변수 설정 (.env 또는 시스템 환경변수)
export DB_URL=jdbc:postgresql://localhost:5432/ems
export DB_USERNAME=ems
export DB_PASSWORD=ems
export JWT_SECRET=your-256-bit-secret-key
export QUARTZ_DB_URL=jdbc:postgresql://localhost:5432/ems
export QUARTZ_DB_USER=ems
export QUARTZ_DB_PASSWORD=ems

# 빌드
./gradlew build -x test

# 실행
java -jar build/libs/backend-*.jar

# 또는 Docker
docker build -t ems-backend .
docker run -p 7092:7092 --env-file .env ems-backend
```

**배포 후 확인:**
- `http://localhost:7092/actuator/health` → `{"status":"UP"}`
- `http://localhost:7092/swagger-ui.html` → Swagger UI
- `POST /auth/login` → `{ "username": "admin", "password": "admin" }` → JWT 토큰 발급

---

### Step 4: Frontend 배포

```bash
cd frontend

# 의존성 설치
npm install

# 환경변수 설정
echo "VITE_API_BASE_URL=http://localhost:7092" > .env

# 개발 서버
npm run dev

# 프로덕션 빌드
npm run build
# dist/ 디렉토리를 Nginx/S3 등에 배포
```

---

### Step 5: 초기 설정 (관리자 대시보드)

1. **로그인**: `http://localhost:5173` → admin / admin
2. **비밀번호 변경**: 설정 → 비밀번호 변경
3. **AWS 연동 설정**: 설정 → AWS 설정
   - Gateway Endpoint: CDK 배포 출력의 API Gateway URL
   - Gateway API Key: AWS 콘솔에서 확인한 API Key 값
   - 연결 테스트 → 성공 확인
4. **폴링 주기 설정**: 설정 → 폴링 주기 (기본 2분)
5. **VDM 설정** (선택): 설정 → VDM 토글 (추가 비용 발생)

---

### Step 6: 테넌트 온보딩

1. **테넌트 생성**: 테넌트 관리 → 테넌트 생성 (이름, 도메인, 할당량)
   - SES Identity + ConfigSet 자동 생성
2. **도메인 인증**: DNS에 DKIM CNAME 레코드 추가
3. **인증 확인**: 테넌트 상세 → DKIM 상태 확인 (SUCCESS)
4. **발신자 등록**: 테넌트 상세 → 발신자 관리 → 이메일 추가
5. **테스트 발송**: 서비스 관리 → 테스트 이메일 발송

---

## 운영 관리

### SES Sandbox 해제

프로덕션 사용 시 AWS 콘솔에서 Sandbox 해제 요청:
1. AWS SES 콘솔 → Account dashboard → Request production access
2. Use case 설명, 발송량, bounce 관리 방법 기술
3. 승인 후 일일 발송 한도 50,000건, TPS 14로 확대

### 모니터링

| 항목 | 확인 방법 |
|------|----------|
| 발송 상태 | 대시보드 → 발송 통계 |
| 테넌트 메트릭 | 모니터링 → 테넌트 메트릭 (CloudWatch) |
| AWS 비용 | 모니터링 → AWS 비용 (Cost Explorer) |
| SES 한도 | 대시보드 → 이메일 발송 일간 한도 |
| DLQ | AWS SQS 콘솔 → ems-send-dlq (0건 유지 확인) |

### 롤백

```bash
# CDK 롤백
cd aws/ems-cdk-v2
cdk destroy

# Backend 롤백
# 이전 버전 JAR 재배포

# Frontend 롤백
# 이전 빌드 dist/ 재배포
```
