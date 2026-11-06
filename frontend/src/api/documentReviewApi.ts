import {
    getCoverLetter,
    getCoverLetterList,
    getPortfolio,
    getPortfolioList,
    getResume,
    getResumeList,
} from './documentApi';
import client from './client';
import type { ApiResponse, DocumentReviewRequestDTO, DocumentReviewResultDTO, DocumentType, Id } from "../types/api";

const documentLoaders = {
    resume: { list: getResumeList, detail: getResume },
    'cover-letter': { list: getCoverLetterList, detail: getCoverLetter },
    portfolio: { list: getPortfolioList, detail: getPortfolio },
};

function loadersFor(documentType: DocumentType) {
    const loaders = documentLoaders[documentType];
    if (!loaders) throw new Error('지원하지 않는 문서 종류입니다.');
    return loaders;
}

export async function getReviewDocuments(documentType: DocumentType) {
    const documents = await loadersFor(documentType).list();
    if (!Array.isArray(documents)) throw new Error('문서 목록을 불러오지 못했습니다.');
    return documents;
}

export function getReviewDocument(documentType: DocumentType, documentNum: Id) {
    return loadersFor(documentType).detail(documentNum);
}

export async function requestDocumentReview(request: DocumentReviewRequestDTO) {
    const response = await client.post<ApiResponse<DocumentReviewResultDTO>>('/document-reviews', request, { timeout: 180000 });
    return response.data.data;
}

export async function getDocumentReview(reviewNum: Id) {
    const response = await client.get<ApiResponse<DocumentReviewResultDTO>>(`/document-reviews/${encodeURIComponent(reviewNum)}`);
    return response.data.data;
}

export async function getDocumentReviewByRequestId(requestId: string) {
    const response = await client.get<ApiResponse<DocumentReviewResultDTO>>(`/document-reviews/requests/${encodeURIComponent(requestId)}`);
    return response.data.data;
}

export async function getDocumentReviewHistory(offset = 0, pageSize = 20) {
    const response = await client.get<ApiResponse<DocumentReviewResultDTO[]>>('/document-reviews', { params: { offset, pageSize } });
    return response.data.data;
}
