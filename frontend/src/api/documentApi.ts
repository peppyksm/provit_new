import client from './client';
import type { ApiResponse, CoverLetterDTO, DocumentSummaryDTO, Id, MainResumeJobInfoDTO, PortfolioDTO, ResumeDetailDTO, ResumeInput } from "../types/api";

export async function createResume(resumeDetail: ResumeInput) {
    const response = await client.post<ApiResponse<ResumeDetailDTO>>('/documents/resumes', resumeDetail);
    return response.data.data;
}

export async function createCoverLetter(coverLetter: Partial<CoverLetterDTO>) {
    const response = await client.post<ApiResponse<CoverLetterDTO>>('/documents/cover-letters', coverLetter);
    return response.data.data;
}

export async function updateResume(resumeNum: Id, resumeDetail: ResumeInput) {
    const response = await client.put<ApiResponse<ResumeDetailDTO>>(`/documents/resumes/${resumeNum}`, resumeDetail);
    return response.data.data;
}

export async function updateCoverLetter(letterNum: Id, coverLetter: Partial<CoverLetterDTO>) {
    const response = await client.put<ApiResponse<CoverLetterDTO>>(`/documents/cover-letters/${letterNum}`, coverLetter);
    return response.data.data;
}

export async function deleteResume(resumeNum: Id) {
    await client.delete<ApiResponse<null>>(`/documents/resumes/${resumeNum}`);
}

export async function deleteCoverLetter(letterNum: Id) {
    await client.delete<ApiResponse<null>>(`/documents/cover-letters/${letterNum}`);
}

export async function deletePortfolio(portfolioNum: Id) {
    await client.delete<ApiResponse<null>>(`/documents/portfolios/${portfolioNum}`);
}

export async function createPortfolio(portfolioTitle: string, file: File) {
    const formData = new FormData();
    formData.append('portfolioTitle', portfolioTitle.trim());
    formData.append('file', file);

    const response = await client.post<ApiResponse<PortfolioDTO>>('/documents/portfolios', formData, {
        timeout: 60000,
    });
    return response.data.data;
}

export async function getResume(resumeNum: Id) {
    const response = await client.get<ApiResponse<ResumeDetailDTO>>(`/documents/resumes/${resumeNum}`);
    return response.data.data;
}

export async function getResumeList() {
    const response = await client.get<ApiResponse<DocumentSummaryDTO[]>>('/documents/resumes');
    return response.data.data;
}

export async function getMainResume() {
    const response = await client.get<ApiResponse<MainResumeJobInfoDTO | null>>('/documents/main-resume');
    return response.data.data;
}

export async function setMainResume(resumeNum: Id) {
    const response = await client.put<ApiResponse<MainResumeJobInfoDTO>>('/documents/main-resume', { resumeNum });
    return response.data.data;
}

export async function clearMainResume() {
    await client.delete<ApiResponse<null>>('/documents/main-resume');
}

export async function getCoverLetter(letterNum: Id) {
    const response = await client.get<ApiResponse<CoverLetterDTO>>(`/documents/cover-letters/${letterNum}`);
    return response.data.data;
}

export async function getCoverLetterList() {
    const response = await client.get<ApiResponse<DocumentSummaryDTO[]>>('/documents/cover-letters');
    return response.data.data;
}

export async function getPortfolio(portfolioNum: Id) {
    const response = await client.get<ApiResponse<PortfolioDTO>>(`/documents/portfolios/${portfolioNum}`);
    return response.data.data;
}

export async function getPortfolioList() {
    const response = await client.get<ApiResponse<DocumentSummaryDTO[]>>('/documents/portfolios');
    return response.data.data;
}

export async function downloadPortfolioFile(portfolioNum: Id) {
    const response = await client.get<Blob>(`/documents/portfolios/${portfolioNum}/file`, {
        responseType: 'blob',
        timeout: 60000,
    });
    return response.data;
}
