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
