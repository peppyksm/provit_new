import assert from 'node:assert/strict';
import { test } from 'node:test';
import { AxiosError } from 'axios';
import client from './client.ts';

async function withAdapter(adapter, verify) {
    const previousAdapter = client.defaults.adapter;
    const previousWindow = globalThis.window;
    globalThis.window = new EventTarget();
    client.defaults.adapter = adapter;
    try { await verify(); } finally {
        client.defaults.adapter = previousAdapter;
        if (previousWindow === undefined) delete globalThis.window;
        else globalThis.window = previousWindow;
    }
}

function reply(config, status) {
    const response = { status, statusText: '', data: {}, headers: {}, config };
    if (status >= 400) throw new AxiosError('request failed', 'ERR_BAD_REQUEST', config, null, response);
    return response;
}

test('보호된 API의 401 응답은 토큰 갱신 후 원래 요청을 한 번 재시도한다', async () => {
    const calls = [];
    await withAdapter(async (config) => {
        calls.push(config.url);
        return reply(config, config.url === '/auth/refresh' || config._retry ? 200 : 401);
    }, async () => assert.equal((await client.post('/studies/1/join')).status, 200));
    assert.deepEqual(calls, ['/studies/1/join', '/auth/refresh', '/studies/1/join']);
});

test('권한 부족 403 응답은 로그인 갱신을 시도하지 않는다', async () => {
    const calls = [];
    await withAdapter(async (config) => { calls.push(config.url); return reply(config, 403); },
        async () => assert.rejects(client.get('/admin/reports'), (error) => error.response.status === 403));
    assert.deepEqual(calls, ['/admin/reports']);
});

test('갱신도 401이면 반복 요청 없이 세션 만료를 알린다', async () => {
    const calls = [];
    let expired = 0;
    await withAdapter(async (config) => { calls.push(config.url); return reply(config, 401); }, async () => {
        window.addEventListener('auth:unauthorized', () => { expired++; });
        await assert.rejects(client.get('/community/posts/me'));
    });
    assert.deepEqual(calls, ['/community/posts/me', '/auth/refresh']);
    assert.equal(expired, 1);
});
