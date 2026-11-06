import client from './client';
import type { ApiResponse, CommentDTO, Id } from "../types/api";
import { requestError } from "./errors";

/**
 * 특정 게시글의 댓글 목록 조회
 */
export const fetchComments = async (postNum: Id) => {
    try {
        const response = await client.get<ApiResponse<CommentDTO[]>>(`/community/posts/${postNum}/comments`);
        return response.data;
    } catch (caughtError) {
        const error = requestError(caughtError);
        console.error('댓글 목록 조회 실패:', error);
        throw error;
    }
};

/**
 * 현재 로그인한 사용자가 작성한 댓글 목록 조회
 */
export const fetchMyComments = async () => {
    try {
        const response = await client.get<ApiResponse<CommentDTO[]>>('/community/comments/me');
        return response.data;
    } catch (caughtError) {
        const error = requestError(caughtError);
        console.error('내 댓글 목록 조회 실패:', error);
        throw error;
    }
};

/**
 * 댓글 작성
 */
export const createComment = async (postNum: Id, commentDto: Partial<CommentDTO>) => {
    try {
        const response = await client.post<ApiResponse<CommentDTO>>(`/community/posts/${postNum}/comments`, commentDto);
        return response.data;
    } catch (caughtError) {
        const error = requestError(caughtError);
        console.error('댓글 작성 실패:', error);
        throw error;
    }
};

/**
 * 댓글 수정
 */
export const updateComment = async (postNum: Id, commentNum: Id, commentDto: Partial<CommentDTO>) => {
    try {
        const response = await client.put<ApiResponse<null>>(`/community/posts/${postNum}/comments/${commentNum}`, commentDto);
        return response.data;
    } catch (caughtError) {
        const error = requestError(caughtError);
        console.error('댓글 수정 실패:', error);
        throw error;
    }
};

/**
 * 댓글 삭제
 */
export const deleteComment = async (postNum: Id, commentNum: Id) => {
    try {
        const response = await client.delete<ApiResponse<null>>(`/community/posts/${postNum}/comments/${commentNum}`);
        return response.data;
    } catch (caughtError) {
        const error = requestError(caughtError);
        console.error('댓글 삭제 실패:', error);
        throw error;
    }
};
