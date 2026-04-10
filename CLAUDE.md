# Project: Message Sending API Server (ESM)

## 프로젝트 구조

```
message-sending-api-server/
├── backend/       # Spring Boot API 서버 (Java 17)
├── frontend/      # React 19 어드민 대시보드 (Vite + TypeScript)
├── docs/          # 프로젝트 문서
└── CLAUDE.md
```

## 문서 관리 정책

개발을 진행하면서 아래 문서를 반드시 함께 업데이트합니다.

| 문서 | 경로 | 업데이트 시점 |
|------|------|--------------|
| Project Overview | `docs/project-overview.md` | 모듈 추가/변경, 시스템 구성 변경, 기술 스택 변경 시 |
| Common Feature Spec | `docs/common-feature-spec.md` | 공통 기능(인증, 에러 처리, Validation, 상태 코드 등) 변경 시 |
| Backend API Spec | `docs/backend-spec.md` | API 엔드포인트 추가/변경/삭제, Request/Response 변경 시 |
| Frontend Spec | `docs/frontend-spec.md` | 프론트엔드 타입, 상태 관리, 라우팅 변경 시 |
| Frontend Dev Plan | `docs/frontend-development-plan.md` | 프론트엔드 Phase별 개발 진행 시 |
| README | `README.md` | 프로젝트 구조, Getting Started, 배포 방식 변경 시 |

### 문서 업데이트 규칙

- 코드 변경 시 관련 문서를 같은 커밋 또는 같은 PR에서 함께 업데이트
- 새로운 API 엔드포인트 추가 시 `backend-spec.md`에 Request/Response 예시 포함
- 새로운 상태 코드, 에러 코드 추가 시 `common-feature-spec.md` 업데이트
- 모듈 추가/아키텍처 변경 시 `project-overview.md`의 시스템 구성도 업데이트
- 프론트엔드 페이지/컴포넌트 추가 시 `frontend-spec.md` 라우팅, 타입 업데이트
- Backend API 변경 시 `frontend-spec.md`의 TypeScript 타입 정의도 함께 업데이트

## 개발 규칙

- Backend: `backend/` 디렉토리에서 작업 (Java 17, Spring Boot 3.4.1)
- Frontend: `frontend/` 디렉토리에서 작업 (React 19, Vite 7, TypeScript 5.9)
- 빌드/실행 명령은 각 디렉토리 내에서 수행
- 커밋 메시지: Conventional Commits 형식 사용 (feat, fix, docs, refactor 등)

### Frontend 개발 규칙
- API 타입은 `src/types/` 에 정의, Backend API Spec과 동기화 유지
- 서버 상태는 TanStack Query, 클라이언트 상태는 Zustand 사용
- 페이지 컴포넌트는 `src/pages/{feature}/` 하위에 배치
- 공통 컴포넌트는 `src/components/` 에 배치

---

## 진행 중인 작업 (AWS SES Native Migration - Phase 1)

> 참조 문서: `docs/v2/ses-native-migration.md`, `docs/v2/ses-native-front-migration.md`

### 완료된 작업

#### Backend
- [x] **코드 정리**: 미사용 파일 삭제 (`SESMariaDBRepository.java`, `PollingNewEmailFromNFTDB.java`)
- [x] **AWS SES 이벤트 타입 정합성**: `EnumSESEventTypeCode` 및 Lambda 함수 (`event-processor`, `email-sender` 등) SES 이벤트명 수정
- [x] **Quartz 버전 업그레이드**: `2.3.x` → `2.5.0`
- [x] **JWT 인증 구현** (Backend):
  - `com.msas.auth` 패키지 신규 생성
  - `JwtProvider.java`: Access/Refresh 토큰 생성·검증
  - `UserEntity.java`, `UserRepository.java` (@Mapper)
  - `AuthController.java`: `/auth/login`, `/auth/refresh`, `/auth/logout`, `/users` CRUD
  - `AuthService.java`: 로그인, 비밀번호 변경, 사용자 관리
  - `JwtAuthenticationFilter.java`: Bearer 토큰 추출 및 SecurityContext 설정
  - `mybatis/user-mapper.xml`: ADM_USER_MST CRUD 쿼리
- [x] **SecurityConfig 수정**: JWT 필터 체인 적용, BCryptPasswordEncoder 빈 등록
- [x] **build.gradle**: jjwt 0.12.6 의존성 추가, Quartz 2.5.0 버전 고정
- [x] **Docker PostgreSQL**: `docker-compose.yml` 신규 생성 (postgres:16-alpine)
- [x] **DB 스키마 (`V1__init_tables.sql`) 수정**:
  - `ADM_USER_MST` 테이블 추가 (admin 초기 계정 포함)
  - `ADM_EMAIL_EVENT_LOG` 테이블 추가
  - `ADM_EMAIL_BL_MST`: `TENANT_ID`, `REASON` 컬럼 추가
  - `TENANT_REGISTRY`: `SES_TENANT_NAME` 컬럼 추가
- [x] **`backend/.env`** 신규 생성 (gitignore 적용, 로컬 DB 접속 정보)
- [x] **빌드 검증**: `./gradlew build` → BUILD SUCCESSFUL 확인

#### Frontend
- [x] **Pro-components 한국어 로케일**: Chinese → Korean (koKRIntl 적용)
- [x] **AWS SES 이벤트 타입 정합성**: `EmailResultStatus` 타입 수정

---

#### Frontend (JWT 로그인)
- [x] `src/types/auth.ts`: JWT 인증 타입 정의
- [x] `src/api/auth.ts`: Auth API 클라이언트
- [x] `src/stores/auth.ts`: API Key → JWT 스토어 전환
- [x] `src/api/client.ts`: JWT 인터셉터 + 자동 토큰 갱신 + 401 큐잉
- [x] `src/pages/auth/LoginPage.tsx`: 로그인 페이지
- [x] `src/components/AuthGuard.tsx`: 라우트 가드 + 30분 비활동 자동 로그아웃
- [x] `src/App.tsx`: `/login` 라우트 + AuthGuard 적용
- [x] `src/layouts/MainLayout.tsx`: 사용자명 표시 + 로그아웃 드롭다운
- [x] `src/pages/settings/index.tsx`: API Key 제거, 비밀번호 변경 추가
- [x] `src/hooks/useAuth.ts`: TanStack Query auth 훅

#### Backend (리팩토링)
- [x] **MonitoringController 서비스 분리**: 393줄 → 89줄 컨트롤러 + `MonitoringService` + `CostEstimateService`
- [x] **ApiResponse<T> 공통 래퍼**: `com.msas.common.dto.ApiResponse` 생성
- [x] **GlobalControllerAdvice 통일**: 모든 에러 응답 `ApiResponse` 포맷
- [x] **Validation 보완**: `RequestUpdateTenantDTO`, `AwsSettingsDTO` 어노테이션 추가
- [x] **SESConfigSetController**: `Map<String, String>` → `RequestConfigSetDTO` + `@Valid`
- [x] **TenantController**: updateTenant/updateQuota `@Valid` 추가

#### 빌드 검증
- [x] Backend: `./gradlew build -x test` → BUILD SUCCESSFUL
- [x] Frontend: `npx tsc --noEmit --pretty` → 에러 0건

#### docs 현행화
- [x] `docs/common-feature-spec.md`: JWT 인증 추가, ApiResponse 포맷, 상태코드 Enum 전환
- [x] `docs/frontend-spec.md`: /login 라우트, auth 타입, AuthGuard, JWT 스토어
- [x] `docs/project-overview.md`: JWT Security, ADM_USER_MST, Auth/Monitoring 모듈
- [x] `docs/backend-spec.md`: Auth API 엔드포인트 추가

---

### Phase 1 완료. 다음: Phase 2 (SES Native 전환)

> Phase 2 계획은 `docs/v2/ses-native-migration.md` 참조
