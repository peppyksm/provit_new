import { useEffect, useState } from 'react';
import { ArrowLeft, FileSearch } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { getCoverLetter, getPortfolio, getResume } from '../../api/documentApi';
import ResumeRead from './components/ResumeRead';
import CoverLetterRead from './components/CoverLetterRead';
import PortfolioRead from './components/PortfolioRead';
import '../documentWrite/DocumentWrite.css';
import './DocumentRead.css';
import { requestError } from "../../api/errors";
import type { DocumentData } from "../../types/api";

const documentConfigs = {
    resume: {
        label: '이력서',
        load: getResume,
        component: ResumeRead,
    },
    'cover-letter': {
        label: '자기소개서',
        load: getCoverLetter,
        component: CoverLetterRead,
    },
    portfolio: {
        label: '포트폴리오',
        load: getPortfolio,
        component: PortfolioRead,
    },
};

function DocumentRead() {
    const { documentType = '', documentId = '' } = useParams();
    const config = (documentType in documentConfigs ? documentConfigs[documentType as keyof typeof documentConfigs] : undefined);
    const [document, setDocument] = useState<DocumentData | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState('');

    useEffect(() => {
        let isActive = true;
        const currentConfig = (documentType in documentConfigs ? documentConfigs[documentType as keyof typeof documentConfigs] : undefined);

        async function loadDocument() {
            if (!currentConfig || !/^\d+$/.test(documentId) || Number(documentId) < 1) {
                if (isActive) {
                    setDocument(null);
                    setErrorMessage('잘못된 문서 주소입니다.');
                    setIsLoading(false);
                }
                return;
            }

            setIsLoading(true);
            setErrorMessage('');
            setDocument(null);
            try {
                const loadedDocument = await currentConfig.load(documentId);
                if (isActive) setDocument(loadedDocument);
            } catch (caughtError) {
                const error = requestError(caughtError);
                if (!isActive) return;
                const responseData = error.response?.data;
                setErrorMessage(
                    (typeof responseData?.data === 'string' ? responseData.data : undefined)
                    || responseData?.responseCode?.message
                    || '문서를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.',
                );
            } finally {
                if (isActive) setIsLoading(false);
            }
        }

        loadDocument();
        return () => {
            isActive = false;
        };
    }, [documentType, documentId]);

    const SelectedDocument = config?.component;

    return (
        <div className="document-write-page document-read-page">
            <Link className="document-read-back" to="/mypage">
                <ArrowLeft size={17} /> 마이페이지로 돌아가기
            </Link>

            <header className="document-write-header">
                <span>DOCUMENT</span>
                <h1>{config ? `${config.label} 조회` : '문서 조회'}</h1>
                <p>저장한 취업 문서의 내용을 확인할 수 있습니다.</p>
            </header>

            {isLoading && (
                <div className="document-read-status" role="status">
                    <span className="document-read-spinner" aria-hidden="true" />
                    <p>문서를 불러오고 있습니다.</p>
                </div>
            )}

            {!isLoading && errorMessage && (
                <div className="document-read-status is-error" role="alert">
                    <FileSearch size={32} aria-hidden="true" />
                    <strong>문서를 표시할 수 없습니다.</strong>
                    <p>{errorMessage}</p>
                    <Link to="/mypage">문서 목록으로 이동</Link>
                </div>
            )}

            {!isLoading && !errorMessage && document && SelectedDocument && (
                'resume' in document ? <ResumeRead document={document} /> : 'letterNum' in document ? <CoverLetterRead document={document} /> : <PortfolioRead document={document} />
            )}
        </div>
    );
}

export default DocumentRead;
