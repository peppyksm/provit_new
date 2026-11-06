import client from './client';
import type { ApiResponse, FileUploadResponseDTO, Id } from "../types/api";

/**
 * 공통 파일 업로드 API 모듈
 * 
 * @param {File} file - 업로드할 File 객체 (input type="file"에서 가져온 파일)
 * @param {'post' | 'profile'} category - 업로드 목적 카테고리
 * @param {number|string} [targetId] - 게시글 번호. 프로필의 경우 로그인 토큰의 userNum 사용.
 * @returns {Promise<{ targetId: number, originalFileName: string, savedFileName: string, fileUrl: string, fileSize: number, category: string, uploadedAt: string }>}
 */
export const uploadFile = async (file: File, category: 'post' | 'profile', targetId: Id | null = null) => {
    const formData = new FormData();
    formData.append('file', file);
    if (targetId !== null && targetId !== undefined) {
        formData.append('targetId', String(targetId));
    }

    const response = await client.post<ApiResponse<FileUploadResponseDTO>>(`/upload/${category}`, formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });

    return response.data.data;
};

/**
 * 저장된 파일 삭제 API 모듈
 * 
 * @param {'post' | 'profile'} category - 소속 카테고리
 * @param {string} savedFileName - 삭제할 저장 파일명
 */
export const deleteFile = async (category: 'post' | 'profile', savedFileName: string) => {
    const response = await client.delete<ApiResponse<null>>(`/upload/${category}/${encodeURIComponent(savedFileName)}`);
    return response.data;
};
