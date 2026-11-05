export const POST_CONTENT_MAX_BYTES = 4000;

export function postContentBytes(content) {
    return new TextEncoder().encode(content || '').length;
}

export function postContentError(content) {
    return postContentBytes(content) > POST_CONTENT_MAX_BYTES
        ? '게시글 본문은 4,000바이트 이내로 입력해 주세요. 내용을 줄인 후 다시 저장해 주세요.'
        : '';
}
