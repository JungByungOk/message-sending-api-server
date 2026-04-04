# Admin API Key DB 관리 설계

**날짜:** 2026-04-04
**상태:** 승인됨

---

## 배경

현재 어드민 포털이 백엔드 API를 호출할 때 사용하는 마스터 API Key가 서버 환경변수(`API_KEY`)로만 관리된다. 운영자가 배포 후 해당 Key 값을 직접 복사하여 어드민 포털 설정 페이지에 수동 입력해야 하는 불편함이 있다.

이를 해소하기 위해 어드민 API Key를 DB에서 관리하고, 최초 설정 및 재발급을 어드민 포털 UI에서 처리할 수 있도록 개선한다.

---

## 목표

- 백엔드 최초 시작 시 어드민 API Key 자동 생성 → DB 저장
- 어드민 포털 최초 접속 시 버튼 하나로 Key 불러오기 및 자동 저장
- 이후 설정 페이지에서 Key 재발급 가능
- 환경변수 의존성 제거

---

## 아키텍처

### 초기 설정 흐름

```
[백엔드 시작]
  DB에 어드민 Key 없음
  → nte_admin_xxx... 자동 생성 → admin_config 테이블에 저장

[어드민 포털 최초 접속]
  API Key 미설정
  → 설정 페이지에 "초기 설정" 안내 배너 표시
  → "Key 가져오기" 버튼 클릭
  → POST /admin/setup-key 호출 (인증 불필요, DB에 Key 없으면 403)
  → Key 반환 → localStorage 자동 저장
  → 이후 정상 인증
```

### 재발급 흐름

```
[설정 페이지]
  "재발급" 버튼 클릭
  → POST /admin/key/regenerate (인증 필요)
  → 새 Key 생성 → DB 업데이트 → 반환
  → localStorage 자동 갱신
```

---

## 상세 설계

### 백엔드

#### DB 테이블: admin_config (V3 마이그레이션)

```sql
CREATE TABLE admin_config (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    config_key  VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT        NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);
-- 초기 데이터 없음 (앱 시작 시 자동 생성)
```

#### 앱 시작 시 Key 자동 생성

`AdminKeyInitializer` (`ApplicationRunner` 구현):
- `admin_config` 테이블에서 `config_key = 'admin_api_key'` 조회
- 없으면 `nte_admin_` 접두사로 SecureRandom Key 생성 → DB 저장
- 서버 로그에 Key 출력 (초기 확인용)

#### SecurityConfig 변경

- 기존: `@Value("${security.api-key}")` 환경변수 조회
- 변경: `AdminConfigRepository`에서 DB 조회
- 기존 테넌트 Key 인증 (`TenantRepository.selectTenantByApiKey`) 유지
- 환경변수 폴백 제거

#### 신규 엔드포인트

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| `POST` | `/admin/setup-key` | 불필요 | DB에 Key 없을 때만 최초 1회 허용 |
| `POST` | `/admin/key/regenerate` | 필요 | Key 재발급 |

**`POST /admin/setup-key`**
- DB에 어드민 Key가 이미 있으면 → `403 Forbidden`
- DB에 Key 없으면 → Key 생성 → DB 저장 → `{ apiKey: "nte_admin_xxx" }` 반환

**`POST /admin/key/regenerate`**
- 인증 필요 (`ROLE_API`)
- 새 Key 생성 → DB 업데이트 → `{ apiKey: "nte_admin_xxx" }` 반환

#### SecurityConfig 인증 제외 경로 추가

```
/admin/setup-key  → permitAll (단, 서비스 레이어에서 DB Key 존재 여부 검증)
```

---

### 프론트엔드

#### 신규 파일

- `src/api/adminKey.ts` — API 클라이언트 (setup-key, regenerate)
- `src/hooks/useAdminKey.ts` — TanStack Query mutation hooks

#### 설정 페이지 UI 변경

**미설정 상태 (API Key 없음):**
```
┌─────────────────────────────────────────┐
│ ⚠ 초기 설정 필요                         │
│ 백엔드에서 API Key를 가져와 저장합니다.    │
│                    [Key 가져오기]         │
└─────────────────────────────────────────┘
```

**설정됨 상태:**
```
Space.Compact:
[ ●●●●●●●●●●  (readOnly Input) ][ 재발급 ][ 초기화 ]
```

- "Key 가져오기": `POST /admin/setup-key` → 성공 시 자동 저장, 이미 설정됨이면 안내 메시지
- "재발급": 확인 Modal → `POST /admin/key/regenerate` → 성공 시 자동 갱신

---

## 보안 고려사항

- `POST /admin/setup-key`는 DB에 Key가 이미 있으면 무조건 `403` 반환 → 중복 호출 불가
- 서버 로그의 Key 출력은 초기 디버깅용, 프로덕션 배포 후 로그 접근 제한 필요
- 재발급 시 기존 Key는 즉시 무효화됨 → 어드민 포털 localStorage 자동 갱신

---

## 변경 파일 목록

### 백엔드
- `backend/src/main/resources/sql/V3__admin_config_table.sql` (신규)
- `backend/src/main/java/com/msas/admin/entity/AdminConfigEntity.java` (신규)
- `backend/src/main/java/com/msas/admin/repository/AdminConfigRepository.java` (신규)
- `backend/src/main/resources/mybatis/admin-config-mapper.xml` (신규)
- `backend/src/main/java/com/msas/admin/service/AdminKeyService.java` (신규)
- `backend/src/main/java/com/msas/admin/controller/AdminKeyController.java` (신규)
- `backend/src/main/java/com/msas/admin/initializer/AdminKeyInitializer.java` (신규)
- `backend/src/main/java/com/msas/common/security/SecurityConfig.java` (수정)
- `backend/src/main/java/com/msas/common/security/ApiKeyAuthenticationFilter.java` (수정)

### 프론트엔드
- `frontend/src/api/adminKey.ts` (신규)
- `frontend/src/hooks/useAdminKey.ts` (신규)
- `frontend/src/pages/settings/index.tsx` (수정)
