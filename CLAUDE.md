# Project: Message Sending API Server (NTE)

## 프로젝트 구조

```
message-sending-api-server/
├── backend/       # Spring Boot API 서버 (Java 17)
├── frontend/      # (예정) 관리 대시보드
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
| Frontend Spec | `docs/frontend-spec.md` | 프론트엔드 개발 시작 시 작성, 이후 기능 변경 시 업데이트 |
| README | `README.md` | 프로젝트 구조, Getting Started, 배포 방식 변경 시 |

### 문서 업데이트 규칙

- 코드 변경 시 관련 문서를 같은 커밋 또는 같은 PR에서 함께 업데이트
- 새로운 API 엔드포인트 추가 시 `backend-spec.md`에 Request/Response 예시 포함
- 새로운 상태 코드, 에러 코드 추가 시 `common-feature-spec.md` 업데이트
- 모듈 추가/아키텍처 변경 시 `project-overview.md`의 시스템 구성도 업데이트

## 개발 규칙

- Backend: `backend/` 디렉토리에서 작업
- Frontend: `frontend/` 디렉토리에서 작업 (개발 시작 시 생성)
- 빌드/실행 명령은 각 디렉토리 내에서 수행
- 커밋 메시지: Conventional Commits 형식 사용 (feat, fix, docs, refactor 등)
