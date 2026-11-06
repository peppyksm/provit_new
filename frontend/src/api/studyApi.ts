import client from './client';
import type { ApiResponse, Id, StudyDTO } from "../types/api";
import { requestError } from "./errors";

export const fetchStudyList = async (userNum?: Id | null) => {
    try {
        const url = userNum ? `/studies?userNum=${userNum}` : `/studies`;
        const response = await client.get<ApiResponse<StudyDTO[]>>(url);
        return response.data;
    } catch (caughtError) {
        const error = requestError(caughtError);
        console.error('스터디 목록 조회 실패:', error);
        throw error;
    }
};

export const createStudy = async (studyDto: Partial<StudyDTO>) => {
    try {
        const response = await client.post<ApiResponse<number>>(`/studies`, studyDto);
        return response.data;
    } catch (caughtError) {
        const error = requestError(caughtError);
        console.error('스터디 개설 실패:', error);
        throw error;
    }
};

export const updateStudy = async (studyNum: Id, studyDto: Partial<StudyDTO>) => {
    try {
        const response = await client.put<ApiResponse<null>>(`/studies/${studyNum}`, studyDto);
        return response.data;
    } catch (caughtError) {
        const error = requestError(caughtError);
        console.error('스터디 수정 실패:', error);
        throw error;
    }
};

export const deleteStudy = async (studyNum: Id) => {
    try {
        const response = await client.delete<ApiResponse<null>>(`/studies/${studyNum}`);
        return response.data;
    } catch (caughtError) {
        const error = requestError(caughtError);
        console.error('스터디 삭제 실패:', error);
        throw error;
    }
};

export const joinStudy = async (studyNum: Id, userNum: Id) => {
    try {
        const response = await client.post<ApiResponse<null>>(`/studies/${studyNum}/join`, { userNum });
        return response.data;
    } catch (caughtError) {
        const error = requestError(caughtError);
        console.error('스터디 참여 실패:', error);
        throw error;
    }
};

export const leaveStudy = async (studyNum: Id, userNum: Id) => {
    try {
        const response = await client.delete<ApiResponse<null>>(`/studies/${studyNum}/leave?userNum=${userNum}`);
        return response.data;
    } catch (caughtError) {
        const error = requestError(caughtError);
        console.error('스터디 탈퇴 실패:', error);
        throw error;
    }
};

export const fetchStudyMembers = async (studyNum: Id) => {
    try {
        const response = await client.get<ApiResponse<string[]>>(`/studies/${studyNum}/members`);
        return response.data;
    } catch (caughtError) {
        const error = requestError(caughtError);
        console.error('스터디 참여자 조회 실패:', error);
        throw error;
    }
};
