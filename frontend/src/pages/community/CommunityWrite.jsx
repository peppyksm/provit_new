import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { createPost, updatePost, deletePost, fetchPostDetail } from '../../api/communityApi';
import { uploadFile, deleteFile } from '../../api/fileApi';
import { useAuth } from '../../context/AuthContext';
import { useModal } from '../../context/ModalContext';
import MDEditor from '@uiw/react-md-editor';
import rehypeSanitize from 'rehype-sanitize';
import { createPostWithAttachment } from './postAttachment';
import { POST_CONTENT_MAX_BYTES, postContentBytes, postContentError } from './postContentLimit';

const CATEGORIES = [
    { id: 1, name: '질문' },
    { id: 2, name: '정보' },
    { id: 3, name: '후기' },
];

function CommunityWrite() {
    const navigate = useNavigate();
    const { user } = useAuth();
    const { showAlert, showConfirm, showToast } = useModal();
    const [loading, setLoading] = useState(false);
    const [file, setFile] = useState(null); // 첨부파일 상태 추가
    const fileInputRef = useRef(null);

    const [formData, setFormData] = useState({
        categoryNum: 1, // 기본값: 질문
        postTitle: '',
        postContent: '',
        postFile: ''
    });
    const [isDirty, setIsDirty] = useState(false); // 폼 수정 여부 추적
    const contentBytes = postContentBytes(formData.postContent);
    const contentError = postContentError(formData.postContent);

    // 브라우저 뒤로가기, 새로고침, 탭 닫기 감지 (Native Event)
    useEffect(() => {
        const handleBeforeUnload = (e) => {
            if (isDirty) {
                e.preventDefault();
                e.returnValue = ''; // Chrome 등 모던 브라우저 규격
            }
        };

        window.addEventListener('beforeunload', handleBeforeUnload);

        return () => {
            window.removeEventListener('beforeunload', handleBeforeUnload);
        };
    }, [isDirty]);

    const handleChange = (e) => {
        setIsDirty(true);
        const { name, value } = e.target;
        setFormData({
            ...formData,
            [name]: name === 'categoryNum' ? parseInt(value) : value
        });
    };

    const handleFileChange = (e) => {
        setIsDirty(true);
        if (e.target.files && e.target.files[0]) {
            setFile(e.target.files[0]);
        } else {
            setFile(null);
        }
    };

    const handleClearFile = () => {
        setFile(null);
        if (fileInputRef.current) {
            fileInputRef.current.value = "";
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!user) {
            await showAlert('로그인이 필요한 기능입니다.', { type: 'warning' });
            navigate('/login');
            return;
        }

        if (!formData.postTitle.trim()) {
            showAlert('제목을 입력해주세요.', { type: 'warning' });
            return;
        }

        if (!formData.postContent.trim()) {
            showAlert('내용을 입력해주세요.', { type: 'warning' });
            return;
        }
        if (contentError) {
            showAlert(contentError, { type: 'warning' });
            return;
        }

        setLoading(true);
        try {
            // 현재 로그인된 유저 번호 매핑
            const postDto = {
                ...formData,
                userNum: user.userNum
            };

            const result = await createPostWithAttachment({ post: postDto, file }, {
                createPost, updatePost, deletePost, uploadFile, deleteFile, fetchPostDetail,
            });
            if (result && result.responseCode && result.responseCode.code === 200) {
                showToast('게시글이 성공적으로 등록되었습니다.', 'success');
                setIsDirty(false); // 서밋 성공 시 경고 해제
                navigate('/community');
            } else {
                showAlert(result?.message || '게시글 등록에 실패했습니다.', { type: 'error' });
            }
        } catch (error) {
            const message = error.response?.data?.data;
            await showAlert(typeof message === 'string' ? message : error.message || '서버와의 통신 오류가 발생했습니다.', { type: 'error' });
            if (error.createdPostNum) {
                setIsDirty(false);
                navigate(`/community/${error.createdPostNum}`);
            }
        } finally {
            setLoading(false);
        }
    };

    const handleCancel = async () => {
        if (!isDirty) {
            navigate('/community');
            return;
        }
        const ok = await showConfirm('작성을 취소하시겠습니까?\n작성 중인 내용은 저장되지 않습니다.', {
            title: '작성 취소',
            type: 'warning',
            confirmText: '작성 취소',
            cancelText: '계속 작성'
        });
        if (ok) {
            navigate('/community');
        }
    };

    return (
        <div className="container py-4" style={{ maxWidth: '800px' }}>
            <h2 className="mb-4 fw-bold">게시글 작성</h2>
            <div className="card shadow-sm">
                <div className="card-body p-4">
                    <form onSubmit={handleSubmit}>
                        <div className="mb-3">
                            <label htmlFor="categoryNum" className="form-label fw-semibold">분류</label>
                            <select
                                className="form-select"
                                id="categoryNum"
                                name="categoryNum"
                                value={formData.categoryNum}
                                onChange={handleChange}
                                required
                            >
                                {CATEGORIES.map(cat => (
                                    <option key={cat.id} value={cat.id}>{cat.name}</option>
                                ))}
                            </select>
                        </div>
                        
                        <div className="mb-3">
                            <label htmlFor="postTitle" className="form-label fw-semibold">제목</label>
                            <input
                                type="text"
                                className="form-control"
                                id="postTitle"
                                name="postTitle"
                                placeholder="제목을 입력해주세요 (최대 100자)"
                                value={formData.postTitle}
                                onChange={handleChange}
                                maxLength={100}
                                required
                            />
                        </div>

                        <div className="mb-3">
                            <label htmlFor="postFile" className="form-label fw-semibold">첨부파일(이미지)</label>
                            <div className="d-flex gap-2 align-items-center">
                                <input
                                    type="file"
                                    className="form-control"
                                    id="postFile"
                                    name="postFile"
                                    accept="image/*"
                                    onChange={handleFileChange}
                                    ref={fileInputRef}
                                />
                                {file && (
                                    <button type="button" className="btn btn-outline-danger btn-sm text-nowrap" onClick={handleClearFile}>
                                        첨부 취소
                                    </button>
                                )}
                            </div>
                            <div className="form-text text-muted">10MB 이하의 이미지 파일(.jpg, .png 등)만 업로드 가능합니다.</div>
                        </div>

                        <div className="mb-4">
                            <label htmlFor="postContent" className="form-label fw-semibold">내용</label>
                            <div data-color-mode="light">
                                <MDEditor
                                    value={formData.postContent}
                                    onChange={(val) => {
                                        setIsDirty(true);
                                        setFormData({ ...formData, postContent: val || '' });
                                    }}
                                    previewOptions={{
                                        rehypePlugins: [[rehypeSanitize]]
                                    }}
                                    height={400}
                                />
                            </div>
                            <div className={`form-text ${contentError ? 'text-danger' : 'text-muted'}`} role={contentError ? 'alert' : undefined}>
                                {contentBytes.toLocaleString()} / {POST_CONTENT_MAX_BYTES.toLocaleString()}바이트 (마크다운 포함)
                                {contentError && <div>{contentError}</div>}
                            </div>
                        </div>

                        <div className="d-flex justify-content-end gap-2 mt-4 pt-3 border-top">
                            <button type="button" className="btn btn-secondary px-4" onClick={handleCancel}>
                                취소
                            </button>
                            <button type="submit" className="btn btn-primary px-4" disabled={loading || Boolean(contentError)}>
                                {loading ? '등록 중...' : '등록'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
}

export default CommunityWrite;
