import assert from 'node:assert/strict';
import test from 'node:test';
import { postContentBytes, postContentError } from './postContentLimit.ts';

test('영문은 4,000바이트까지 허용하고 초과 시 저장 전에 안내한다', () => {
    assert.equal(postContentBytes('A'.repeat(4000)), 4000);
    assert.equal(postContentError('A'.repeat(4000)), '');
    assert.match(postContentError('A'.repeat(4001)), /4,000바이트/);
});

test('한글을 글자 수가 아닌 실제 저장 바이트 수로 검사한다', () => {
    assert.equal(postContentBytes('가'.repeat(1333)), 3999);
    assert.equal(postContentError('가'.repeat(1333)), '');
    assert.match(postContentError('가'.repeat(1334)), /4,000바이트/);
    assert.match(postContentError('가'.repeat(1400)), /4,000바이트/);
});

test('이모지와 한영 혼합 본문의 저장 한도를 일치시킨다', () => {
    assert.equal(postContentBytes('😀'.repeat(1000)), 4000);
    assert.equal(postContentError('😀'.repeat(1000)), '');
    assert.notEqual(postContentError('😀'.repeat(1001)), '');
    assert.equal(postContentError('가'.repeat(1332) + 'ABCD'), '');
    assert.notEqual(postContentError('가'.repeat(1332) + 'ABCDE'), '');
});

test('마크다운과 공백도 저장 용량에 포함하고 입력 내용을 자르지 않는다', () => {
    const content = '**' + '가'.repeat(1332) + '**';
    assert.equal(postContentBytes(content), 4000);
    assert.equal(postContentError(content), '');
    assert.notEqual(postContentError(content + '\n'), '');
    assert.equal(content.length, 1336);
});
