import assert from 'node:assert/strict';
import { registerHooks } from 'node:module';
import test, { after } from 'node:test';
import { createElement } from 'react';
import { renderToString } from 'react-dom/server';
import { MemoryRouter } from 'react-router-dom';

// Node renders the components; Vite normally loads these non-JavaScript assets.
const assets = registerHooks({
  load(url, context, nextLoad) {
    if (/\.(css|png|svg)$/.test(url)) {
      return { format: 'module', source: `export default ${JSON.stringify(url)}`, shortCircuit: true };
    }
    return nextLoad(url, context);
  },
});
const previousStorage = Object.getOwnPropertyDescriptor(globalThis, 'localStorage');
const values = new Map();
Object.defineProperty(globalThis, 'localStorage', { configurable: true, value: {
  getItem: (key) => values.get(key) ?? null,
  setItem: (key, value) => values.set(key, String(value)),
  removeItem: (key) => values.delete(key),
} });
after(() => {
  assets.deregister();
  if (previousStorage) Object.defineProperty(globalThis, 'localStorage', previousStorage);
  else delete globalThis.localStorage;
});
const { AuthProvider } = await import('./context/AuthContext.tsx');
const { ModalProvider } = await import('./context/ModalContext.tsx');
const cases = [
  ['pages/home/Home.tsx', '/', ''],
  ['pages/auth/Login.tsx', '/login', '로그인'],
  ['pages/auth/Signup.tsx', '/signup', '회원가입'],
  ['pages/auth/PasswordReset.tsx', '/password-reset', '비밀번호'],
  ['pages/community/CommunityList.tsx', '/community', ''],
  ['pages/jobs/JobList.tsx', '/jobs', ''],
  ['pages/mbti/Mbti.tsx', '/mbti', '질문'],
  ['pages/documentWrite/DocumentWrite.tsx', '/documents/write?type=resume', '이력서'],
  ['pages/documentWrite/DocumentWrite.tsx', '/documents/write?type=cover-letter', '자기소개서'],
  ['pages/documentWrite/DocumentWrite.tsx', '/documents/write?type=portfolio', '포트폴리오'],
  ['pages/documentReview/DocumentReview.tsx', '/document-review', '첨삭'],
  ['pages/interview/Interview.tsx', '/interview', '면접'],
  ['pages/mypage/MyPage.tsx', '/mypage', ''],
];
for (const [file, url, label] of cases) {
  test(`TypeScript 화면 초기 렌더링: ${url}`, async () => {
    const { default: Component } = await import(new URL(file, import.meta.url).href);
    const html = renderToString(createElement(MemoryRouter, { initialEntries: [url] },
      createElement(AuthProvider, null, createElement(ModalProvider, null, createElement(Component)))));
    assert.ok(html.length > 20);
    assert.ok(html.includes(label), `${url}: ${label}`);
  });
}
