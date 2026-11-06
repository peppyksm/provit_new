import client from './client';
import type { ApiResponse, Id, InterviewAnswerRequestDTO, InterviewAnswerResponseDTO, InterviewAvailabilityDTO, InterviewDocumentResponseDTO, InterviewRecordDTO, InterviewRecruitmentDTO, InterviewResultDTO, InterviewSessionResponseDTO, InterviewSettings, InterviewStartResponseDTO } from "../types/api";

const INTERVIEW_TIMEOUT = 150000;

export async function getInterviewAvailability() {
    const response = await client.get<ApiResponse<InterviewAvailabilityDTO>>('/interview/availability');
    return response.data.data;
}

export async function getInterviewSession(historyNum: Id) {
    const response = await client.get<ApiResponse<InterviewSessionResponseDTO>>(`/interview/${historyNum}/session`, { timeout: INTERVIEW_TIMEOUT });
    return response.data.data;
}

export async function discardInterviewSession(historyNum: Id) {
    await client.delete<ApiResponse<null>>(`/interview/${historyNum}/session`, { timeout: INTERVIEW_TIMEOUT });
}

export async function getInterviewResults() {
    const response = await client.get<ApiResponse<InterviewResultDTO[]>>('/interview/results');
    return response.data.data;
}

export async function getInterviewRecord(historyNum: Id) {
    const response = await client.get<ApiResponse<InterviewRecordDTO>>(`/interview/${historyNum}/record`);
    return response.data.data;
}

export async function getInterviewDocuments() {
    const response = await client.get<ApiResponse<InterviewDocumentResponseDTO>>('/interview/documents');
    return response.data.data;
}

export async function createInterview(settings: InterviewSettings, requestId: string, recruitment: InterviewRecruitmentDTO | null = null) {
    const response = await client.post<ApiResponse<InterviewStartResponseDTO>>('/interview/start', {
        resumeNum: Number(settings.resumeNum),
        portfolioNum: Number(settings.portfolioNum || 0),
        letterNum: Number(settings.letterNum || 0),
        interviewDifficulty: settings.difficulty,
        requestId,
        recruitment,
    }, { timeout: INTERVIEW_TIMEOUT });
    return response.data.data;
}

export async function submitInterviewAnswer(historyNum: Id, answerRequest: InterviewAnswerRequestDTO) {
    const response = await client.post<ApiResponse<InterviewAnswerResponseDTO>>(`/interview/${historyNum}/answers`, answerRequest, {
        timeout: INTERVIEW_TIMEOUT,
    });
    return response.data.data;
}
