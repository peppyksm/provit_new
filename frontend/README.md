# Provit 프론트엔드

React 19 + TypeScript 6 + Vite 8을 사용합니다. Node.js 24 LTS에서 검증했습니다.

```bash
npm ci
npm run dev
```

개발 서버는 `http://localhost:5173`, 백엔드는 `http://localhost:8080`입니다. `/api`, `/uploads` 요청은 Vite가 백엔드로 전달합니다.

| 명령 | 용도 |
| --- | --- |
| `npm run typecheck` | 전체 앱·Vite 설정의 엄격한 타입 검사 |
| `npm test` | 기존 회귀 테스트·오류 처리·화면 초기 렌더링 검사 |
| `npm run build` | 타입 검사 후 `dist/` 생성 |
| `npm run lint` | TypeScript·React 규칙 검사 |
| `npm run preview` | 빌드 결과 미리보기 |

컴포넌트는 `.tsx`, 일반 모듈은 `.ts`로 작성합니다. API의 JSON 타입은 `src/types/api.ts`에서 관리합니다. 기존 `.test.mjs` 테스트는 `tsx`를 통해 실제 TypeScript 모듈을 실행합니다.

전체 린트에는 이전 JavaScript 코드부터 있던 React 훅·빠른 새로고침 관련 지적 등이 남아 있습니다. [전환 범위와 검증 결과](../docs/typescript-migration.md)를 참고하세요.
