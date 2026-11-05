function requireSuccess(result, fallbackMessage) {
    if (result?.responseCode?.code === 200) return result;
    const message = typeof result?.data === 'string' ? result.data : result?.responseCode?.message;
    const error = new Error(message || fallbackMessage);
    error.response = { status: 200, data: result };
    throw error;
}

async function readPost(postNum, api) {
    try {
        return requireSuccess(await api.fetchPostDetail(postNum), '게시글을 확인하지 못했습니다.').data;
    } catch {
        return null;
    }
}

function matchesSavedPost(current, requested) {
    return current
        && current.postTitle === requested.postTitle
        && current.postContent === requested.postContent
        && Number(current.categoryNum) === Number(requested.categoryNum)
        && (current.postFile || '') === (requested.postFile || '');
}

function isConfirmedRejection(error) {
    return [200, 400, 401, 403, 404, 409, 413, 415, 422].includes(error.response?.status);
}

async function cleanUpFile(savedFileName, api) {
    try {
        requireSuccess(await api.deleteFile('post', savedFileName), '첨부파일을 정리하지 못했습니다.');
    } catch (error) {
        // 파일 정리 실패가 이미 완료된 게시글 저장을 실패로 바꾸지 않게 한다.
        console.error('게시글 첨부파일 정리 실패:', error);
    }
}

export async function savePostAttachment({ post, file, originalPostFile = '', removeAttachment = false }, api) {
    let uploadedFileName = '';
    if (file) {
        const uploaded = await api.uploadFile(file, 'post', post.postNum);
        if (!uploaded?.savedFileName) throw new Error('업로드된 첨부파일을 확인하지 못했습니다.');
        uploadedFileName = uploaded.savedFileName;
    }

    const requested = {
        ...post,
        postFile: uploadedFileName || (removeAttachment ? '' : originalPostFile),
    };
    let result;
    try {
        result = requireSuccess(await api.updatePost(requested), '게시글 저장에 실패했습니다.');
    } catch (error) {
        const current = await readPost(post.postNum, api);
        if (matchesSavedPost(current, requested)) {
            // 응답만 유실됐을 수 있으므로 실제 저장 결과를 확인한 후 정리한다.
            result = { responseCode: { code: 200 }, data: null };
        } else {
            if (uploadedFileName && isConfirmedRejection(error) && current && current.postFile !== uploadedFileName) {
                await cleanUpFile(uploadedFileName, api);
            }
            // 응답 유실이나 중계 서버 오류 뒤에도 원래 저장 요청이 진행 중일 수 있다.
            if (!isConfirmedRejection(error) || !current || (uploadedFileName && current.postFile === uploadedFileName)) {
                error.attachmentSaveUncertain = true;
            }
            throw error;
        }
    }

    if (originalPostFile && originalPostFile !== requested.postFile) {
        await cleanUpFile(originalPostFile, api);
    }
    return result;
}

export async function createPostWithAttachment({ post, file }, api) {
    const result = requireSuccess(await api.createPost(post), '게시글 등록에 실패했습니다.');
    if (!file) return result;

    const postNum = result.data;
    try {
        await savePostAttachment({ post: { ...post, postNum }, file }, api);
        return result;
    } catch (error) {
        if (!error.attachmentSaveUncertain) {
            try {
                requireSuccess(await api.deletePost(postNum), '게시글 등록을 취소하지 못했습니다.');
            } catch {
                const remaining = new Error('첨부파일 저장에 실패했습니다. 게시글 상태를 확인해 주세요.', { cause: error });
                remaining.createdPostNum = postNum;
                throw remaining;
            }
            throw new Error('첨부파일 저장에 실패하여 게시글 등록을 취소했습니다.', { cause: error });
        }
        const uncertain = new Error('첨부파일 저장 여부를 확인하지 못했습니다. 게시글에서 저장 상태를 확인해 주세요.', { cause: error });
        uncertain.createdPostNum = postNum;
        throw uncertain;
    }
}
