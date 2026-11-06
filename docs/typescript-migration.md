# JavaScript → TypeScript 전환

## 전환 범위

애플리케이션의 JavaScript·JSX 파일 86개를 `.ts`·`.tsx`로 전환했다. 화면 구성, URL, API 경로와 쿠키 인증 흐름은 유지한다. Vite 설정도 `vite.config.ts`로 변경했다.

- `src/types/api.ts`: 실제 백엔드 DTO의 JSON 이름과 날짜 문자열, nullable 필드, 목록·페이징·문서·면접 타입을 정의한다. 서버에서 `@JsonIgnore` 처리하는 토큰·파일 저장 경로 등은 공개 응답 타입에서 제외한다.
- API 모듈과 화면의 직접 Axios 요청에 응답 타입을 지정했다. 인증 갱신용 `skipAuthRefresh`, `_retry` 옵션도 Axios 타입에 반영했다.
- Context, 컴포넌트 속성, 입력 이벤트, 배열·빈 상태, DOM 참조와 비동기 리소스 상태에 타입을 지정했다. 사용자 정보 갱신 함수는 함수형 상태 갱신도 지원한다.
- 오류는 `unknown`에서 처리한다. `api/errors.ts`는 기존 Error 객체와 첨부파일 복구 정보를 유지하고, HTTP 상태·면접 복구 플래그를 읽는다. 서버가 첨삭 요청을 찾지 못한 경우에만 같은 요청을 재전송하는 동작도 유지한다.
- `tsconfig.json`은 `strict`, `isolatedModules`, `verbatimModuleSyntax`, `noEmit`을 사용한다. 앱 소스에 `any`, `@ts-ignore`, `@ts-nocheck`를 추가하지 않았다.
- `build`는 `tsc --noEmit` 성공 후 Vite 빌드를 수행한다. Vite의 TypeScript 변환만으로는 타입 검사가 실행되지 않으므로 별도 검사를 포함했다. [Vite 공식 문서](https://vite.dev/guide/features.html#typescript)

## 실행과 검증

Node.js 24.14.1에서 검증했다. 프로젝트 루트에서 다음 명령을 실행할 수 있다.

```bash
npm run typecheck
npm test
npm run build
npm run lint
```

- 타입 검사: 통과.
- 테스트: 70개 통과. 기존 회귀 테스트 53개, 오류 처리 4개, 주요 화면 초기 렌더링 13개다.
- 배포 빌드: 통과. 결과물은 `frontend/dist/`에 생성한다.
- 초기 렌더링 검사는 홈, 인증 화면, 커뮤니티, 채용공고, MBTI, 문서 작성 3종, 첨삭, 면접, 마이페이지를 포함한다. 브라우저 클릭·실제 서버 요청·유료 AI 호출을 검증하는 테스트는 아니다.

기존 테스트는 `.test.mjs`를 유지하고 `node --import tsx --test`로 전환된 소스를 실행한다. 화면 초기 렌더링 테스트는 Node에서 CSS·이미지 모듈을 대체하며, 이 처리는 테스트에만 적용된다.

## 기존 린트 지적

전체 린트는 기존 코드에서 이어진 오류 21개·경고 10개 때문에 실패한다. 동일한 현재 검사 규칙으로 전환 전 HEAD의 JavaScript 소스를 검사하면 오류 40개·경고 10개였다. 남은 규칙별 지적은 다음과 같다.

| 규칙 | 개수 |
| --- | ---: |
| `react-hooks/set-state-in-effect` | 13 |
| `react-hooks/immutability` | 5 |
| `react-refresh/only-export-components` | 2 |
| `@typescript-eslint/no-unused-vars` | 1 |
| `react-hooks/exhaustive-deps` 경고 | 10 |

타입 검사와 전체 린트는 별도 명령이다. 기존 React 효과·함수 선언 구조를 전면 변경하거나 해당 규칙을 끄지는 않았다.

## 이후 개발 기준

새 화면은 `.tsx`, 공통 함수·API는 `.ts`로 작성한다. 서버 DTO나 JSON 응답을 바꿀 때는 공통 타입과 호출부를 함께 수정하고 타입 검사·테스트·빌드를 실행한다. 타입 선언이 외부 응답의 런타임 검증을 대신하지는 않으므로, 기존의 응답·브라우저 저장소 검사도 유지한다. [TypeScript 공식 전환 안내](https://www.typescriptlang.org/docs/handbook/migrating-from-javascript.html)
