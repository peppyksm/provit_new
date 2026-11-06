import { useEffect, useState } from 'react';
import { getDocumentReview, getDocumentReviewByRequestId, getReviewDocument, getReviewDocuments } from '../../api/documentReviewApi';
import { reviewErrorMessage } from './documentReviewConfig';
import { requestError } from "../../api/errors";
import type { DocumentData, DocumentReviewResultDTO, DocumentSummaryDTO, DocumentType, ReviewResource } from "../../types/api";

export function useReviewDocuments(documentType: DocumentType) {
    const [revision, setRevision] = useState(0);
    const [resource, setResource] = useState<ReviewResource<DocumentSummaryDTO[]> | null>(null);
    const resourceKey = `${documentType}:${revision}`;

    useEffect(() => {
        let active = true;
        getReviewDocuments(documentType).then(
            (data) => {
                if (active) setResource({ key: resourceKey, data });
            },
            (error) => {
                if (active)
                    setResource({
                        key: resourceKey,
                        error: reviewErrorMessage(error, '문서 목록을 불러오지 못했습니다.'),
                    });
            },
        );
        return () => {
            active = false;
        };
    }, [documentType, resourceKey]);

    const current = resource?.key === resourceKey ? resource : null;
    return {
        documents: current?.data || [],
        isLoading: !current,
        error: current?.error || '',
        reload: () => setRevision((value) => value + 1),
    };
}

export function useReviewDocument(documentType: DocumentType, documentNum: string) {
    const [revision, setRevision] = useState(0);
    const [resource, setResource] = useState<ReviewResource<DocumentData> | null>(null);
    const resourceKey = `${documentType}:${documentNum}:${revision}`;

    useEffect(() => {
        if (!documentNum) return undefined;
        let active = true;
        getReviewDocument(documentType, documentNum).then(
            (data) => {
                if (active)
                    setResource(
                        data
                            ? { key: resourceKey, data }
                            : { key: resourceKey, error: '선택한 문서의 내용을 불러오지 못했습니다.' },
                    );
            },
            (error) => {
                if (active)
                    setResource({ key: resourceKey, error: reviewErrorMessage(error, '문서를 불러오지 못했습니다.') });
            },
        );
        return () => {
            active = false;
        };
    }, [documentType, documentNum, resourceKey]);

    const current = resource?.key === resourceKey ? resource : null;
    return {
        document: current?.data || null,
        isLoading: Boolean(documentNum) && !current,
        error: current?.error || '',
        reload: () => setRevision((value) => value + 1),
    };
}

export function useReviewSelection(documentType: DocumentType, requestedDocumentNum: string) {
    const list = useReviewDocuments(documentType);
    const selectedDocumentNum = list.documents.some((document) => String(document.documentNum) === requestedDocumentNum)
        ? requestedDocumentNum
        : '';
    const source = useReviewDocument(documentType, selectedDocumentNum);
    return { ...list, selectedDocumentNum, requestedDocumentNum, source };
}

export function useSavedReview(reviewNum: string, initialResult: DocumentReviewResultDTO | null | undefined, requestId = '') {
    const [revision, setRevision] = useState(0);
    const [resource, setResource] = useState<ReviewResource<DocumentReviewResultDTO> | null>(null);
    const resourceKey = `${reviewNum || requestId}:${revision}`;
    const cachedResult = revision === 0 && String(initialResult?.reviewNum) === reviewNum ? initialResult : null;
    useEffect(() => {
        if ((!reviewNum && !requestId) || (cachedResult && cachedResult.reviewStatus !== 'PROCESSING')) return undefined;
        let active = true;
        let timer: ReturnType<typeof setTimeout> | undefined;
        const refresh = async () => {
            try {
                const data = reviewNum ? await getDocumentReview(reviewNum) : await getDocumentReviewByRequestId(requestId);
                if (!active) return;
                setResource({ key: resourceKey, data });
                if (data.reviewStatus === 'PROCESSING') timer = setTimeout(refresh, 3000);
            } catch (caughtError) {
        const error = requestError(caughtError);
                if (active) setResource({ key: resourceKey, error: reviewErrorMessage(error, '기존 첨삭 요청을 확인하지 못했습니다. 요청 상태를 다시 확인해 주세요.') });
            }
        };
        refresh();
        return () => { active = false; clearTimeout(timer); };
    }, [reviewNum, requestId, resourceKey, cachedResult]);
    const current = resource?.key === resourceKey ? resource : cachedResult ? { data: cachedResult, error: '' } : null;
    return {
        result: current?.data || null,
        isLoading: Boolean(reviewNum || requestId) && !current,
        error: current?.error || '',
        reload: () => setRevision((value) => value + 1),
    };
}
