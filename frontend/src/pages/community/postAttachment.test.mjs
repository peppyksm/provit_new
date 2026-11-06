import assert from 'node:assert/strict';
import { mock, test } from 'node:test';
import { createPostWithAttachment, savePostAttachment } from './postAttachment.ts';
import { deleteFile } from '../../api/fileApi.ts';
import client from '../../api/client.ts';

const success = (data = null) => ({ responseCode: { code: 200 }, data });
const oldFile = '42_old.png';
const newFile = '42_new.png';
const file = { name: 'new.png' };
const post = { postNum: 42, categoryNum: 1, postTitle: '수정 제목', postContent: '수정 내용', userNum: 7 };
function rejected(status = 400) {
    return Object.assign(new Error('저장 실패'), { response: { status, data: { data: '저장 실패' } } });
}

function fixture() {
    const calls = [];
    const files = new Set([oldFile]);
    let current = { ...post, postTitle: '기존 제목', postFile: oldFile };
    const api = {
        async uploadFile(_file, category, postNum) {
            assert.equal(category, 'post');
            assert.equal(postNum, 42);
            calls.push('upload');
            files.add(newFile);
            return { savedFileName: newFile };
        },
        async updatePost(requested) {
            calls.push('save');
            current = { ...requested };
            return success();
        },
        async fetchPostDetail(postNum) {
            assert.equal(postNum, 42);
            calls.push('read');
            return success(current);
        },
        async deleteFile(category, name) {
            assert.equal(category, 'post');
            calls.push(`delete:${name}`);
            assert.notEqual(name, current?.postFile, '현재 게시글의 첨부파일을 삭제하면 안 된다');
            files.delete(name);
            return success();
        },
        async createPost(requested) {
            calls.push('create');
            current = { ...requested, postNum: 42, postFile: '' };
            return success(42);
        },
        async deletePost(postNum) {
            assert.equal(postNum, 42);
            calls.push('delete-post');
            current = null;
            return success();
        },
    };
    return { api, calls, files, current: () => current,
        save: (options = {}) => savePostAttachment({ post, file, originalPostFile: oldFile, ...options }, api),
        create: (selectedFile = file) => createPostWithAttachment({ post, file: selectedFile }, api) };
}

test('파일 교체는 새 파일 업로드와 게시글 저장이 끝난 뒤 기존 파일을 삭제한다', async () => {
    const f = fixture();
    await f.save();
    assert.deepEqual(f.calls, ['upload', 'save', `delete:${oldFile}`]);
    assert.equal(f.current().postFile, newFile);
    assert.deepEqual([...f.files], [newFile]);
});

test('첨부파일 삭제도 게시글에서 연결을 제거한 뒤 실제 파일을 삭제한다', async () => {
    const f = fixture();
    await f.save({ file: null, removeAttachment: true });
    assert.deepEqual(f.calls, ['save', `delete:${oldFile}`]);
    assert.equal(f.current().postFile, '');
    assert.equal(f.files.has(oldFile), false);
});

test('첨부파일을 바꾸지 않는 수정은 기존 파일을 유지한다', async () => {
    const f = fixture();
    await f.save({ file: null });
    assert.deepEqual(f.calls, ['save']);
    assert.equal(f.current().postFile, oldFile);
});

test('새 파일 업로드 실패 시 기존 파일과 게시글을 변경하지 않는다', async () => {
    const f = fixture();
    f.api.uploadFile = async () => { f.calls.push('upload'); throw rejected(); };
    await assert.rejects(f.save());
    assert.deepEqual(f.calls, ['upload']);
    assert.equal(f.current().postFile, oldFile);
    assert.equal(f.files.has(oldFile), true);
});

test('교체 저장이 거부되면 새 파일만 정리하고 기존 파일을 보존한다', async () => {
    const f = fixture();
    const error = rejected();
    f.api.updatePost = async () => { f.calls.push('save'); throw error; };
    await assert.rejects(f.save(), (actual) => actual === error);
    assert.deepEqual(f.calls, ['upload', 'save', 'read', `delete:${newFile}`]);
    assert.equal(f.current().postFile, oldFile);
    assert.deepEqual([...f.files], [oldFile]);
});

test('삭제 저장이 거부되면 기존 파일을 삭제하지 않는다', async () => {
    const f = fixture();
    f.api.updatePost = async () => { f.calls.push('save'); throw rejected(); };
    await assert.rejects(f.save({ file: null, removeAttachment: true }));
    assert.deepEqual(f.calls, ['save', 'read']);
    assert.equal(f.files.has(oldFile), true);
});

test('HTTP 200이어도 응답 본문이 실패이면 저장 성공으로 처리하지 않는다', async () => {
    const f = fixture();
    f.api.updatePost = async () => ({ responseCode: { code: 318, message: '인증 실패' }, data: null });
    await assert.rejects(f.save(), /인증 실패/);
    assert.deepEqual([...f.files], [oldFile]);
});

test('기존 파일 정리 실패가 완료된 게시글 저장을 실패로 바꾸지 않는다', async () => {
    const f = fixture();
    f.api.deleteFile = async () => { throw new Error('삭제 실패'); };
    const log = mock.method(console, 'error', () => {});
    try {
        assert.equal((await f.save()).responseCode.code, 200);
        assert.equal(f.current().postFile, newFile);
        assert.equal(f.files.has(newFile), true);
        assert.equal(log.mock.callCount(), 1);
    } finally { log.mock.restore(); }
});

test('실패한 업로드 파일 정리에서 오류가 나도 원래 저장 오류를 보존한다', async () => {
    const f = fixture();
    const error = rejected();
    f.api.updatePost = async () => { throw error; };
    f.api.deleteFile = async () => { throw new Error('정리 실패'); };
    const log = mock.method(console, 'error', () => {});
    try {
        await assert.rejects(f.save(), (actual) => actual === error);
        assert.equal(f.files.has(oldFile), true);
    } finally { log.mock.restore(); }
});

test('저장은 완료되고 응답만 유실됐으면 저장 결과를 확인해 새 파일을 유지한다', async () => {
    const f = fixture();
    const save = f.api.updatePost;
    f.api.updatePost = async (requested) => { await save(requested); throw new Error('응답 유실'); };
    assert.equal((await f.save()).responseCode.code, 200);
    assert.deepEqual(f.calls, ['upload', 'save', 'read', `delete:${oldFile}`]);
    assert.deepEqual([...f.files], [newFile]);
});

test('응답 없이 저장이 진행 중일 수 있으면 기존 파일과 새 파일을 모두 유지한다', async () => {
    const f = fixture();
    f.api.updatePost = async () => { f.calls.push('save'); throw new Error('시간 초과'); };
    await assert.rejects(f.save(), (error) => error.attachmentSaveUncertain === true);
    assert.deepEqual(f.calls, ['upload', 'save', 'read']);
    assert.equal(f.files.has(oldFile), true);
    assert.equal(f.files.has(newFile), true);
});

test('저장 결과 조회도 실패하면 첨부파일을 임의로 삭제하지 않는다', async () => {
    const f = fixture();
    f.api.updatePost = async () => { throw rejected(500); };
    f.api.fetchPostDetail = async () => { throw new Error('조회 실패'); };
    await assert.rejects(f.save(), (error) => error.attachmentSaveUncertain === true);
    assert.deepEqual([...f.files], [oldFile, newFile]);
});

test('중계 서버의 시간 초과 응답 뒤 원래 저장 요청이 계속될 수 있으므로 파일을 유지한다', async () => {
    const f = fixture();
    f.api.updatePost = async () => { throw rejected(504); };
    await assert.rejects(f.save(), (error) => error.attachmentSaveUncertain === true);
    assert.deepEqual([...f.files], [oldFile, newFile]);
    assert.equal(f.calls.some((call) => call.startsWith('delete:')), false);
});

test('새 파일이 이미 연결돼 있으면 다른 필드가 달라도 파일을 삭제하지 않는다', async () => {
    const f = fixture();
    const save = f.api.updatePost;
    f.api.updatePost = async (requested) => {
        await save({ ...requested, postTitle: '다른 수정 내용' });
        throw rejected(500);
    };
    await assert.rejects(f.save(), (error) => error.attachmentSaveUncertain === true);
    assert.deepEqual([...f.files], [oldFile, newFile]);
});

test('첨부파일 없는 새 게시글은 추가 업로드나 수정 없이 등록한다', async () => {
    const f = fixture();
    assert.equal((await f.create(null)).data, 42);
    assert.deepEqual(f.calls, ['create']);
});

test('첨부파일 있는 새 게시글은 파일 연결까지 성공해야 등록 성공을 반환한다', async () => {
    const f = fixture();
    assert.equal((await f.create()).data, 42);
    assert.deepEqual(f.calls, ['create', 'upload', 'save']);
    assert.equal(f.current().postFile, newFile);
});

test('새 게시글의 업로드 실패 시 생성한 게시글을 취소한다', async () => {
    const f = fixture();
    f.api.uploadFile = async () => { f.calls.push('upload'); throw rejected(); };
    await assert.rejects(f.create(), /등록을 취소했습니다/);
    assert.deepEqual(f.calls, ['create', 'upload', 'delete-post']);
    assert.equal(f.current(), null);
});

test('새 게시글의 파일 연결 실패 시 새 파일부터 정리한 뒤 게시글을 취소한다', async () => {
    const f = fixture();
    f.api.updatePost = async () => { f.calls.push('save'); throw rejected(); };
    await assert.rejects(f.create(), /등록을 취소했습니다/);
    assert.deepEqual(f.calls, ['create', 'upload', 'save', 'read', `delete:${newFile}`, 'delete-post']);
    assert.equal(f.files.has(newFile), false);
});

test('새 게시글 저장 여부가 불확실하면 게시글과 새 파일을 보존하고 확인할 번호를 반환한다', async () => {
    const f = fixture();
    f.api.updatePost = async () => { throw new Error('시간 초과'); };
    await assert.rejects(f.create(), (error) => error.createdPostNum === 42 && /저장 여부/.test(error.message));
    assert.equal(f.current().postNum, 42);
    assert.equal(f.files.has(newFile), true);
    assert.equal(f.calls.includes('delete-post'), false);
});

test('새 게시글의 저장 완료 응답이 유실됐어도 게시글을 취소하지 않는다', async () => {
    const f = fixture();
    const save = f.api.updatePost;
    f.api.updatePost = async (requested) => { await save(requested); throw new Error('응답 유실'); };
    assert.equal((await f.create()).data, 42);
    assert.equal(f.current().postFile, newFile);
    assert.equal(f.calls.includes('delete-post'), false);
});

test('게시글 취소도 실패하면 취소됐다고 안내하지 않고 생성된 게시글을 확인하게 한다', async () => {
    const f = fixture();
    f.api.uploadFile = async () => { throw rejected(); };
    f.api.deletePost = async () => { throw rejected(500); };
    await assert.rejects(f.create(), (error) => error.createdPostNum === 42 && !/취소했습니다/.test(error.message));
});

test('한글·공백·특수문자가 있는 첨부파일 삭제 주소를 올바르게 인코딩한다', async () => {
    const name = '42_첨부 #1 +사진.png';
    const remove = mock.method(client, 'delete', async (url) => {
        assert.equal(url, `/upload/post/${encodeURIComponent(name)}`);
        return { data: success() };
    });
    try { await deleteFile('post', name); } finally { remove.mock.restore(); }
});
