# PostgreSQL 테이블 명세

2026-11-05 기준 `provit_new`의 실제 PostgreSQL 18 카탈로그에서 확인한 정의다. 테이블 27개·시퀀스 19개를 사용한다.

초기화 원본: `backend/src/main/resources/sql_query/schema.sql`, 직군·직무 기준정보: `seed_saramin_jobs.sql`. [전환·설정·검증 안내](docs/postgresql-migration.md).

날짜 전용 컬럼은 `date`, 생성·수정·만료 시각은 `timestamp without time zone`, 긴 원문·JSON 문자열은 `text`, PDF는 `bytea`다. JSON 문자열에는 유효성 CHECK를 적용한다. 게시글 본문은 기존 UTF-8 4,000바이트 제한도 CHECK로 유지한다.

## T_CAREER

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| CAREER_NUM | bigint | 예 | nextval('seq_t_career'::regclass) | NOT NULL career_num; PRIMARY KEY (career_num) |
| RESUME_NUM | bigint | 예 |  | FOREIGN KEY (resume_num) REFERENCES t_resume(resume_num) ON DELETE CASCADE; NOT NULL resume_num |
| COMPANY_NAME | character varying(200) | 예 |  | NOT NULL company_name |
| JOIN_DATE | date |  |  |  |
| RESIGN_DATE | date |  |  |  |
| MAIN_DUTY | character varying(2000) |  |  |  |

## T_CATEGORY

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| CATEGORY_NUM | integer | 예 | nextval('seq_t_category'::regclass) | NOT NULL category_num; PRIMARY KEY (category_num) |
| CATEGORY_NAME | character varying(100) | 예 |  | NOT NULL category_name |

## T_CERTIFICATION

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| CERT_NUM | bigint | 예 | nextval('seq_t_certification'::regclass) | NOT NULL cert_num; PRIMARY KEY (cert_num) |
| RESUME_NUM | bigint | 예 |  | FOREIGN KEY (resume_num) REFERENCES t_resume(resume_num) ON DELETE CASCADE; NOT NULL resume_num |
| CERT_NAME | character varying(200) | 예 |  | NOT NULL cert_name |
| CERT_GRADE | character varying(100) |  |  |  |
| ISSUE_DATE | date |  |  |  |

## T_COMMENT

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| COMMENT_NUM | bigint | 예 | nextval('seq_t_comment'::regclass) | NOT NULL comment_num; PRIMARY KEY (comment_num) |
| POST_NUM | bigint | 예 |  | FOREIGN KEY (post_num) REFERENCES t_post(post_num) ON DELETE CASCADE; NOT NULL post_num |
| USER_NUM | integer | 예 |  | FOREIGN KEY (user_num) REFERENCES t_user(user_num) ON DELETE CASCADE; NOT NULL user_num |
| COMMENT_CONTENT | character varying(2000) | 예 |  | NOT NULL comment_content |
| COMMENT_DATE | timestamp without time zone | 예 | LOCALTIMESTAMP | NOT NULL comment_date |
| PARENT_COMMENT_NUM | bigint |  |  | FOREIGN KEY (parent_comment_num) REFERENCES t_comment(comment_num) |
| IS_DELETED | smallint | 예 | 0 | NOT NULL is_deleted |

## T_COVER_LETTER

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| LETTER_NUM | bigint | 예 | nextval('seq_t_cover_letter'::regclass) | NOT NULL letter_num; PRIMARY KEY (letter_num) |
| USER_NUM | integer | 예 |  | FOREIGN KEY (user_num) REFERENCES t_user(user_num) ON DELETE CASCADE; NOT NULL user_num |
| COVER_LETTER_TITLE | character varying(200) | 예 |  | NOT NULL cover_letter_title |
| GROWTH_PROCESS | character varying(3000) |  |  |  |
| PERSONALITY_STRENGTHS_WEAKNESSES | character varying(3000) |  |  |  |
| PROBLEM_SOLVING_EXPERIENCE | character varying(3000) |  |  |  |
| POST_JOINING_ASPIRATION | character varying(3000) |  |  |  |
| CREATED_AT | timestamp without time zone | 예 | LOCALTIMESTAMP | NOT NULL created_at |
| UPDATED_AT | timestamp without time zone | 예 | LOCALTIMESTAMP | NOT NULL updated_at |

## T_DOCUMENT_REVIEW

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| REVIEW_NUM | bigint | 예 | nextval('seq_t_document_review'::regclass) | NOT NULL review_num; PRIMARY KEY (review_num) |
| USER_NUM | integer | 예 |  | FOREIGN KEY (user_num) REFERENCES t_user(user_num) ON DELETE CASCADE; NOT NULL user_num |
| REQUEST_ID | character varying(36) |  |  | CHECK (request_id IS NULL AND request_hash IS NULL OR request_id IS NOT NULL AND request_hash IS NOT NULL) |
| REQUEST_HASH | character varying(64) |  |  | CHECK (request_id IS NULL AND request_hash IS NULL OR request_id IS NOT NULL AND request_hash IS NOT NULL) |
| REVIEW_TITLE | character varying(200) | 예 |  | NOT NULL review_title |
| REVIEW_STATUS | character varying(20) | 예 | 'PROCESSING'::character varying | CHECK (review_status::text = 'PROCESSING'::text AND finished_at IS NULL AND error_message IS NULL OR review_status::text = 'COMPLETED'::text AND finished_at IS NOT NULL AND error_message IS NULL OR review_status::text = 'FAILED'::text AND finished_at IS NOT NULL AND error_message IS NOT NULL); NOT NULL review_status |
| REVIEW_MODE | character varying(20) | 예 | 'comprehensive'::character varying | CHECK (review_mode::text = 'custom'::text AND custom_criteria IS NOT NULL AND custom_criteria::text ~ '[^[:space:]]'::text OR review_mode::text <> 'custom'::text AND custom_criteria IS NULL); CHECK (review_mode::text = ANY (ARRAY['comprehensive'::character varying, 'expression'::character varying, 'consistency'::character varying, 'jobFit'::character varying, 'evidence'::character varying, 'custom'::character varying]::text[])); NOT NULL review_mode |
| CUSTOM_CRITERIA | character varying(200) |  |  | CHECK (review_mode::text = 'custom'::text AND custom_criteria IS NOT NULL AND custom_criteria::text ~ '[^[:space:]]'::text OR review_mode::text <> 'custom'::text AND custom_criteria IS NULL) |
| INSTRUCTIONS | character varying(200) |  |  |  |
| OVERALL_SUMMARY | text |  |  |  |
| CAREER_PREPARATION_JSON | text |  |  | CHECK (career_preparation_json IS JSON) |
| MODEL_NAME | character varying(100) |  |  |  |
| PROMPT_VERSION | character varying(50) |  |  |  |
| RESPONSE_VERSION | smallint | 예 | 1 | CHECK (response_version > 0); NOT NULL response_version |
| ERROR_MESSAGE | character varying(1000) |  |  | CHECK (review_status::text = 'PROCESSING'::text AND finished_at IS NULL AND error_message IS NULL OR review_status::text = 'COMPLETED'::text AND finished_at IS NOT NULL AND error_message IS NULL OR review_status::text = 'FAILED'::text AND finished_at IS NOT NULL AND error_message IS NOT NULL) |
| CREATED_AT | timestamp(6) without time zone | 예 | LOCALTIMESTAMP | CHECK (finished_at >= created_at); NOT NULL created_at |
| FINISHED_AT | timestamp(6) without time zone |  |  | CHECK (review_status::text = 'PROCESSING'::text AND finished_at IS NULL AND error_message IS NULL OR review_status::text = 'COMPLETED'::text AND finished_at IS NOT NULL AND error_message IS NULL OR review_status::text = 'FAILED'::text AND finished_at IS NOT NULL AND error_message IS NOT NULL); CHECK (finished_at >= created_at) |

## T_EDUCATION

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| EDU_NUM | bigint | 예 | nextval('seq_t_education'::regclass) | NOT NULL edu_num; PRIMARY KEY (edu_num) |
| RESUME_NUM | bigint | 예 |  | FOREIGN KEY (resume_num) REFERENCES t_resume(resume_num) ON DELETE CASCADE; NOT NULL resume_num |
| SCHOOL_NAME | character varying(200) | 예 |  | NOT NULL school_name |
| ADMISSION_DATE | date |  |  |  |
| GRADUATION_DATE | date |  |  |  |
| MAJOR | character varying(200) |  |  |  |
| EDUCATION_STATUS | character varying(20) | 예 |  | NOT NULL education_status |

## T_INTERVIEW_HISTORY

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| HISTORY_NUM | bigint | 예 | nextval('seq_t_interview_history'::regclass) | NOT NULL history_num; PRIMARY KEY (history_num) |
| USER_NUM | integer | 예 |  | FOREIGN KEY (user_num) REFERENCES t_user(user_num) ON DELETE CASCADE; NOT NULL user_num |
| QUESTION1 | character varying(120) |  |  |  |
| ANSWER1 | character varying(1000) |  |  |  |
| QUESTION2 | character varying(120) |  |  |  |
| ANSWER2 | character varying(1000) |  |  |  |
| QUESTION3 | character varying(120) |  |  |  |
| ANSWER3 | character varying(1000) |  |  |  |
| QUESTION4 | character varying(120) |  |  |  |
| ANSWER4 | character varying(1000) |  |  |  |
| QUESTION5 | character varying(120) |  |  |  |
| ANSWER5 | character varying(1000) |  |  |  |
| INTERVIEW_DATE | timestamp without time zone | 예 | LOCALTIMESTAMP | NOT NULL interview_date |

## T_INTERVIEW_RESULT

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| HISTORY_NUM | bigint | 예 |  | FOREIGN KEY (history_num) REFERENCES t_interview_history(history_num) ON DELETE CASCADE; NOT NULL history_num; PRIMARY KEY (history_num, user_num) |
| USER_NUM | integer | 예 |  | FOREIGN KEY (user_num) REFERENCES t_user(user_num) ON DELETE CASCADE; NOT NULL user_num; PRIMARY KEY (history_num, user_num) |
| DOCUMENT_CONSISTENCY_SCORE | numeric(5,2) |  | 0.00 |  |
| EXPERTISE_SCORE | numeric(5,2) |  | 0.00 |  |
| PROBLEM_SOLVING_SCORE | numeric(5,2) |  | 0.00 |  |
| LOGIC_SCORE | numeric(5,2) |  | 0.00 |  |
| COMMUNICATION_SCORE | numeric(5,2) |  | 0.00 |  |
| TOTAL_SCORE | numeric(5,2) |  | 0.00 |  |
| STRENGTH | character varying(250) |  |  |  |
| WEAKNESS | character varying(250) |  |  |  |
| PREVIOUS_COMPARISON | character varying(250) |  |  |  |
| IMPROVEMENT_POINT | character varying(250) |  |  |  |
| INTERVIEW_DATE | timestamp without time zone | 예 | LOCALTIMESTAMP | NOT NULL interview_date |

## T_JOB

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| JOB_CODE | character varying(20) | 예 |  | NOT NULL job_code; PRIMARY KEY (job_code) |
| OCCUPATION_CODE | character varying(20) | 예 |  | FOREIGN KEY (occupation_code) REFERENCES t_occupation(occupation_code) ON DELETE CASCADE; NOT NULL occupation_code |
| JOB_NAME | character varying(200) | 예 |  | NOT NULL job_name |

## T_JOB_SCRAP

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| RECRUITMENT_NUM | bigint | 예 |  | FOREIGN KEY (recruitment_num) REFERENCES t_recruitment(recruitment_num) ON DELETE CASCADE; NOT NULL recruitment_num; PRIMARY KEY (recruitment_num, user_num) |
| USER_NUM | integer | 예 |  | FOREIGN KEY (user_num) REFERENCES t_user(user_num) ON DELETE CASCADE; NOT NULL user_num; PRIMARY KEY (recruitment_num, user_num) |
| CREATED_AT | timestamp without time zone | 예 | LOCALTIMESTAMP | NOT NULL created_at |

## T_OCCUPATION

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| OCCUPATION_CODE | character varying(20) | 예 |  | NOT NULL occupation_code; PRIMARY KEY (occupation_code) |
| OCCUPATION_NAME | character varying(200) | 예 |  | NOT NULL occupation_name |

## T_PORTFOLIO

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| PORTFOLIO_NUM | bigint | 예 | nextval('seq_t_portfolio'::regclass) | NOT NULL portfolio_num; PRIMARY KEY (portfolio_num) |
| USER_NUM | integer | 예 |  | FOREIGN KEY (user_num) REFERENCES t_user(user_num) ON DELETE CASCADE; NOT NULL user_num |
| PORTFOLIO_TITLE | character varying(200) | 예 |  | NOT NULL portfolio_title |
| FILE_URL | character varying(500) |  |  |  |
| ORIGINAL_FILE_NAME | character varying(255) |  |  |  |
| SAVED_FILE_NAME | character varying(255) |  |  |  |
| CREATED_AT | timestamp without time zone | 예 | LOCALTIMESTAMP | NOT NULL created_at |
| UPDATED_AT | timestamp without time zone | 예 | LOCALTIMESTAMP | NOT NULL updated_at |

## T_POST

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| POST_NUM | bigint | 예 | nextval('seq_t_post'::regclass) | NOT NULL post_num; PRIMARY KEY (post_num) |
| CATEGORY_NUM | integer | 예 |  | FOREIGN KEY (category_num) REFERENCES t_category(category_num) ON DELETE CASCADE; NOT NULL category_num |
| USER_NUM | integer | 예 |  | FOREIGN KEY (user_num) REFERENCES t_user(user_num) ON DELETE CASCADE; NOT NULL user_num |
| POST_TITLE | character varying(300) | 예 |  | NOT NULL post_title |
| POST_CONTENT | character varying(4000) |  |  | CHECK (octet_length(post_content::text) <= 4000) |
| POST_LIKE_COUNT | integer | 예 | 0 | NOT NULL post_like_count |
| VIEW_COUNT | integer | 예 | 0 | NOT NULL view_count |
| POST_FILE | character varying(500) |  |  |  |
| POST_DATE | timestamp without time zone | 예 | LOCALTIMESTAMP | NOT NULL post_date |

## T_POST_LIKE

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| POST_NUM | bigint | 예 |  | FOREIGN KEY (post_num) REFERENCES t_post(post_num) ON DELETE CASCADE; NOT NULL post_num; PRIMARY KEY (post_num, user_num) |
| USER_NUM | integer | 예 |  | FOREIGN KEY (user_num) REFERENCES t_user(user_num) ON DELETE CASCADE; NOT NULL user_num; PRIMARY KEY (post_num, user_num) |
| CREATED_AT | timestamp without time zone | 예 | LOCALTIMESTAMP | NOT NULL created_at |

## T_RECRUITMENT

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| RECRUITMENT_NUM | bigint | 예 | nextval('seq_t_recruitment'::regclass) | NOT NULL recruitment_num; PRIMARY KEY (recruitment_num) |
| SARAMIN_JOB_ID | character varying(50) | 예 |  | NOT NULL saramin_job_id; UNIQUE (saramin_job_id) |
| COMPANY_NAME | character varying(200) | 예 |  | NOT NULL company_name |
| TITLE | character varying(400) | 예 |  | NOT NULL title |
| JOB_URL | character varying(500) | 예 |  | NOT NULL job_url |
| LOCATION_NAME | character varying(200) |  |  |  |
| JOB_NAME | character varying(300) |  |  |  |
| EXPERIENCE_LEVEL | character varying(100) |  |  |  |
| EXPIRATION_DATE | timestamp without time zone |  |  |  |
| CLOSE_TYPE | character varying(50) |  |  |  |
| IS_ACTIVE | smallint | 예 | 1 | NOT NULL is_active |
| CREATED_AT | timestamp without time zone | 예 | LOCALTIMESTAMP | NOT NULL created_at |

## T_REFRESH_TOKEN

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| TOKEN_ID | character varying(36) | 예 |  | NOT NULL token_id; PRIMARY KEY (token_id) |
| USER_NUM | integer | 예 |  | FOREIGN KEY (user_num) REFERENCES t_user(user_num) ON DELETE CASCADE; NOT NULL user_num |
| EXPIRES_AT | timestamp without time zone | 예 |  | NOT NULL expires_at |

## T_REPORT

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| REPORT_NUM | bigint | 예 | nextval('seq_t_report'::regclass) | NOT NULL report_num; PRIMARY KEY (report_num) |
| REPORTER_NUM | integer |  |  | FOREIGN KEY (reporter_num) REFERENCES t_user(user_num) ON DELETE SET NULL |
| TARGET_TYPE | character varying(20) | 예 |  | NOT NULL target_type |
| TARGET_NUM | bigint | 예 |  | NOT NULL target_num |
| REPORT_REASON | character varying(100) | 예 |  | NOT NULL report_reason |
| REPORT_STATUS | character varying(20) | 예 | 'PENDING'::character varying | NOT NULL report_status |
| REPORT_DATE | timestamp without time zone | 예 | LOCALTIMESTAMP | NOT NULL report_date |

## T_RESUME

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| RESUME_NUM | bigint | 예 | nextval('seq_t_resume'::regclass) | NOT NULL resume_num; PRIMARY KEY (resume_num) |
| USER_NUM | integer | 예 |  | FOREIGN KEY (user_num) REFERENCES t_user(user_num) ON DELETE CASCADE; NOT NULL user_num |
| RESUME_TITLE | character varying(200) |  |  |  |
| OCCUPATION_CODE | character varying(20) |  |  | FOREIGN KEY (occupation_code) REFERENCES t_occupation(occupation_code) ON DELETE SET NULL |
| JOB_CODE | character varying(20) |  |  | FOREIGN KEY (job_code) REFERENCES t_job(job_code) ON DELETE SET NULL |
| HIGHEST_LEVEL | character varying(20) | 예 |  | NOT NULL highest_level |
| MOTIVATION | text |  |  |  |
| DESIRED_LOCATION | character varying(200) |  |  |  |
| DESIRED_WORK_TYPE | character varying(100) |  |  |  |
| CREATED_AT | timestamp without time zone | 예 | LOCALTIMESTAMP | NOT NULL created_at |
| UPDATED_AT | timestamp without time zone | 예 | LOCALTIMESTAMP | NOT NULL updated_at |

## T_REVIEW_CONSISTENCY

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| CONSISTENCY_NUM | bigint | 예 | nextval('seq_t_review_consistency'::regclass) | NOT NULL consistency_num; PRIMARY KEY (consistency_num); UNIQUE (review_num, consistency_num) |
| REVIEW_NUM | bigint | 예 |  | FOREIGN KEY (review_num) REFERENCES t_document_review(review_num) ON DELETE CASCADE; NOT NULL review_num; UNIQUE (review_num, display_order); UNIQUE (review_num, consistency_num) |
| DISPLAY_ORDER | integer | 예 |  | CHECK (display_order > 0); NOT NULL display_order; UNIQUE (review_num, display_order) |
| ISSUE_TYPE | character varying(30) | 예 |  | CHECK (issue_type::text = ANY (ARRAY['mismatch'::character varying, 'missingEvidence'::character varying, 'needsConfirmation'::character varying]::text[])); NOT NULL issue_type |
| ISSUE_TITLE | character varying(500) | 예 |  | NOT NULL issue_title |
| RECOMMENDATION | text | 예 |  | NOT NULL recommendation |

## T_REVIEW_DOCUMENT

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| REVIEW_DOCUMENT_NUM | bigint | 예 | nextval('seq_t_review_document'::regclass) | NOT NULL review_document_num; PRIMARY KEY (review_document_num); UNIQUE (review_num, review_document_num) |
| REVIEW_NUM | bigint | 예 |  | FOREIGN KEY (review_num) REFERENCES t_document_review(review_num) ON DELETE CASCADE; NOT NULL review_num; UNIQUE (review_num, review_document_num); UNIQUE (review_num, document_type) |
| DOCUMENT_TYPE | character varying(20) | 예 |  | CHECK (document_type::text = ANY (ARRAY['resume'::character varying, 'cover-letter'::character varying, 'portfolio'::character varying]::text[])); NOT NULL document_type; UNIQUE (review_num, document_type) |
| SOURCE_DOCUMENT_NUM | bigint | 예 |  | CHECK (source_document_num > 0); NOT NULL source_document_num |
| DOCUMENT_TITLE | character varying(200) | 예 |  | NOT NULL document_title |
| SOURCE_UPDATED_AT | timestamp without time zone |  |  |  |
| SOURCE_SNAPSHOT_JSON | text | 예 |  | CHECK (source_snapshot_json IS JSON); NOT NULL source_snapshot_json |
| ORIGINAL_FILE_NAME | character varying(255) |  |  |  |
| PDF_SNAPSHOT | bytea |  |  |  |
| DOCUMENT_SUMMARY | text |  |  |  |

## T_REVIEW_IMPROVEMENT

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| IMPROVEMENT_NUM | bigint | 예 | nextval('seq_t_review_improvement'::regclass) | NOT NULL improvement_num; PRIMARY KEY (improvement_num) |
| REVIEW_DOCUMENT_NUM | bigint | 예 |  | FOREIGN KEY (review_document_num) REFERENCES t_review_document(review_document_num) ON DELETE CASCADE; NOT NULL review_document_num; UNIQUE (review_document_num, display_order) |
| DISPLAY_ORDER | integer | 예 |  | CHECK (display_order > 0); NOT NULL display_order; UNIQUE (review_document_num, display_order) |
| SECTION_NAME | character varying(500) | 예 |  | NOT NULL section_name |
| IMPROVEMENT_TITLE | character varying(500) | 예 |  | NOT NULL improvement_title |
| ISSUE_CONTENT | text | 예 |  | NOT NULL issue_content |
| ORIGINAL_CONTENT | text | 예 |  | NOT NULL original_content |
| SUGGESTED_CONTENT | text | 예 |  | NOT NULL suggested_content |
| REASON_CONTENT | text | 예 |  | NOT NULL reason_content |

## T_REVIEW_SOURCE

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| CONSISTENCY_NUM | bigint | 예 |  | FOREIGN KEY (review_num, consistency_num) REFERENCES t_review_consistency(review_num, consistency_num) ON DELETE CASCADE; NOT NULL consistency_num; PRIMARY KEY (consistency_num, display_order) |
| DISPLAY_ORDER | integer | 예 |  | CHECK (display_order > 0); NOT NULL display_order; PRIMARY KEY (consistency_num, display_order) |
| REVIEW_NUM | bigint | 예 |  | FOREIGN KEY (review_num, review_document_num) REFERENCES t_review_document(review_num, review_document_num) ON DELETE CASCADE; FOREIGN KEY (review_num, consistency_num) REFERENCES t_review_consistency(review_num, consistency_num) ON DELETE CASCADE; NOT NULL review_num |
| REVIEW_DOCUMENT_NUM | bigint | 예 |  | FOREIGN KEY (review_num, review_document_num) REFERENCES t_review_document(review_num, review_document_num) ON DELETE CASCADE; NOT NULL review_document_num |
| SECTION_NAME | character varying(500) | 예 |  | NOT NULL section_name |
| SOURCE_CONTENT | text | 예 |  | NOT NULL source_content |
| PAGE_NUMBER | integer |  |  | CHECK (page_number > 0) |

## T_REVIEW_STRENGTH

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| STRENGTH_NUM | bigint | 예 | nextval('seq_t_review_strength'::regclass) | NOT NULL strength_num; PRIMARY KEY (strength_num) |
| REVIEW_NUM | bigint | 예 |  | FOREIGN KEY (review_num, review_document_num) REFERENCES t_review_document(review_num, review_document_num) ON DELETE CASCADE; FOREIGN KEY (review_num) REFERENCES t_document_review(review_num) ON DELETE CASCADE; NOT NULL review_num |
| REVIEW_DOCUMENT_NUM | bigint |  |  | FOREIGN KEY (review_num, review_document_num) REFERENCES t_review_document(review_num, review_document_num) ON DELETE CASCADE |
| DISPLAY_ORDER | integer | 예 |  | CHECK (display_order > 0); NOT NULL display_order |
| STRENGTH_CONTENT | text | 예 |  | NOT NULL strength_content |
| STRENGTH_REASON | text |  |  |  |
| STRENGTH_SUGGESTION | text |  |  |  |
| STRENGTH_SOURCES_JSON | text |  |  | CHECK (strength_sources_json IS JSON) |

## T_STUDY

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| STUDY_NUM | bigint | 예 | nextval('seq_t_study'::regclass) | NOT NULL study_num; PRIMARY KEY (study_num) |
| USER_NUM | integer | 예 |  | FOREIGN KEY (user_num) REFERENCES t_user(user_num) ON DELETE CASCADE; NOT NULL user_num |
| STUDY_NAME | character varying(200) | 예 |  | NOT NULL study_name |
| STUDY_EXPLAIN | character varying(2000) |  |  |  |
| MAX_MEMBERS | smallint | 예 | 10 | CHECK (max_members >= 2); NOT NULL max_members |
| STUDY_CREATE_DATE | timestamp without time zone | 예 | LOCALTIMESTAMP | NOT NULL study_create_date |

## T_STUDY_MEMBER

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| STUDY_NUM | bigint | 예 |  | FOREIGN KEY (study_num) REFERENCES t_study(study_num) ON DELETE CASCADE; NOT NULL study_num; PRIMARY KEY (study_num, user_num) |
| USER_NUM | integer | 예 |  | FOREIGN KEY (user_num) REFERENCES t_user(user_num) ON DELETE CASCADE; NOT NULL user_num; PRIMARY KEY (study_num, user_num) |
| STUDY_JOIN_DATE | date | 예 | LOCALTIMESTAMP | NOT NULL study_join_date |

## T_USER

| 컬럼 | 타입 | 필수 | 기본값 | 제약조건 |
| --- | --- | --- | --- | --- |
| USER_NUM | integer | 예 | nextval('seq_t_user'::regclass) | NOT NULL user_num; PRIMARY KEY (user_num) |
| USER_NAME | character varying(100) | 예 |  | NOT NULL user_name |
| USER_NICKNAME | character varying(100) | 예 |  | NOT NULL user_nickname |
| USER_BIRTH_DATE | date |  |  |  |
| USER_EMAIL | character varying(200) | 예 |  | NOT NULL user_email; UNIQUE (user_email) |
| USER_PW | character varying(255) | 예 |  | NOT NULL user_pw |
| USER_REGISTER_DATE | timestamp without time zone | 예 | LOCALTIMESTAMP | NOT NULL user_register_date |
| USER_TYPE | character varying(30) |  | 'USER'::character varying |  |
| USER_IS_DELETED | smallint | 예 | 0 | NOT NULL user_is_deleted |
| USER_TOKEN_VERSION | integer | 예 | 0 | NOT NULL user_token_version |
| BLOCKED_DATE | timestamp without time zone |  |  |  |
| MAIN_RESUME_NUM | bigint |  |  | FOREIGN KEY (main_resume_num) REFERENCES t_resume(resume_num) ON DELETE SET NULL |
| OCCUPATION_CODE | character varying(20) |  |  | FOREIGN KEY (occupation_code) REFERENCES t_occupation(occupation_code) ON DELETE SET NULL |
| JOB_CODE | character varying(20) |  |  | FOREIGN KEY (job_code) REFERENCES t_job(job_code) ON DELETE SET NULL |
