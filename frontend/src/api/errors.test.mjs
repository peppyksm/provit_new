import assert from 'node:assert/strict';
import test from 'node:test';
import { AxiosError } from 'axios';
import { requestError, errorDetails } from './errors.ts';

test('Axios 오류와 첨부파일 복구 정보를 가진 Error는 원래 객체를 유지한다', () => {
  const axiosError = new AxiosError('timeout', 'ECONNABORTED');
  const uploadError = Object.assign(new Error('save'), { attachmentSaveUncertain: true, createdPostNum: 42 });
  assert.equal(requestError(axiosError), axiosError);
  assert.equal(requestError(uploadError), uploadError);
});

test('일반 객체로 전달된 실패도 HTTP 상태와 면접 복구 정보를 읽는다', () => {
  const error = requestError({ message: 'expired', response: { status: 409, data: {
    responseCode: { code: 409, message: '세션 만료' }, data: { restartRequired: true, answerLocked: true },
  } } });
  assert.equal(error.response.status, 409);
  assert.equal(error.response.data.responseCode.message, '세션 만료');
  assert.equal(errorDetails(error).restartRequired, true);
  assert.equal(errorDetails(error).answerLocked, true);
});

test('문자열 오류 메시지는 복구 플래그로 해석하지 않는다', () => {
  const error = requestError({ response: { status: 400, data: { data: '잘못된 입력' } } });
  assert.equal(error.response.data.data, '잘못된 입력');
  assert.deepEqual(errorDetails(error), {});
});

test('알 수 없는 예외와 잘못된 복구 필드는 안전하게 처리한다', () => {
  for (const value of [null, undefined, 'offline', 0]) assert.ok(requestError(value) instanceof Error);
  assert.deepEqual(errorDetails({ response: { status: 400, data: { data: { restartRequired: 'true' } } } }), {
    message: undefined, maintenance: false, restartRequired: false, answerLocked: false,
  });
});
