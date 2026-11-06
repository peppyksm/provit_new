// JSON contracts shared by the API modules and screens. Dates arrive as strings.
export type Id = number | string;
export type DocumentType = "resume" | "cover-letter" | "portfolio";
export interface AdminReportDTO {
  reportNum: number;
  reporterNum: number;
  reporterNickname: string;
  targetType: string;
  targetNum: number;
  targetContentPreview: string;
  commentPostNum: number | null;
  reportReason: string;
  reportStatus: string;
  reportDate: string;
}
export interface AdminReportRequestDTO {
  reportStatus: string;
}
export interface AdminUserDTO {
  userNum: number;
  userEmail: string;
  userName: string;
  userNickname: string;
  userType: string;
  userRegisterDate: string;
  userIsDeleted: number;
  blockedDate: string | null;
}
export interface AdminUserRequestDTO {
  blockDays: number;
}
export interface EmailSendRequestDTO {
  email: string;
}
export interface EmailVerifyRequestDTO {
  email: string;
  code: string;
}
export interface LoginRequestDTO {
  userEmail: string;
  userPw: string;
}
export interface LoginResponseDTO {
  user: UserResponseDTO;
}
export interface MyPageUpdateRequestDTO {
  userNickname: string;
  currentPassword: string;
  newPassword: string;
  newPasswordConfirm: string;
}
export interface PasswordResetRequestDTO {
  verificationToken: string;
  newPassword: string;
  confirmPassword: string;
}
export interface SignupRequestDTO {
  userName: string;
  userNickname: string;
  userEmail: string;
  userPw: string;
  confirmPw: string;
  userBirthDate: string;
  occupationCode: string;
  jobCode: string;
  verificationToken: string;
}
export interface UserResponseDTO {
  userNum: number;
  userName: string;
  userNickname: string;
  userBirthDate: string | null;
  userEmail: string;
  userRegisterDate: string;
  userType: string | null;
  occupationCode: string | null;
  jobCode: string | null;
  mainResumeNum?: number | null;
}
export interface WithdrawalRequestDTO {
  currentPassword: string;
}
export interface PageResponseDTO<T> {
  list: T[];
  totalCount: number;
  currentPage: number;
  pageSize: number;
  totalPages: number;
  hasNext: boolean;
  hasPrev: boolean;
}
export interface CommentDTO {
  commentNum: number;
  postNum: number;
  userNum: number;
  commentContent: string;
  commentDate: string;
  userNickname: string;
  postTitle: string;
  parentCommentNum: number | null;
  isDeleted: number;
}
export interface PostDTO {
  postNum: number;
  categoryNum: number;
  userNum: number;
  postTitle: string;
  postContent: string;
  postLikeCount: number;
  viewCount: number;
  postFile: string | null;
  isLiked: boolean;
  isBlinded: boolean;
  postDate: string;
  categoryName: string;
  userNickname: string;
  commentCount: number;
}
export interface PostSearchDTO {
  categoryNum: number;
  searchType: string;
  keyword: string;
  authorUserNum: number;
  page: number;
  pageSize: number;
}
export interface ReportDTO {
  reportNum: number;
  reporterNum: number;
  targetType: string;
  targetNum: number;
  reportReason: string;
  reportStatus: string;
  reportDate: string;
}
export interface CareerDTO {
  careerNum: number;
  resumeNum: number;
  companyName: string;
  joinDate: string | null;
  resignDate: string | null;
  mainDuty: string | null;
}
export interface CertificationDTO {
  certNum: number;
  resumeNum: number;
  certName: string;
  certGrade: string | null;
  issueDate: string | null;
}
export interface CoverLetterDTO {
  letterNum: number;
  userNum: number;
  coverLetterTitle: string;
  growthProcess: string | null;
  personalityStrengthsWeaknesses: string | null;
  problemSolvingExperience: string | null;
  postJoiningAspiration: string | null;
  createdAt: string;
  updatedAt: string;
}
export interface DocumentReviewDTO {
  reviewNum: number;
  requestId: string;
  reviewTitle: string;
  reviewStatus: string;
  errorMessage: string | null;
  reviewMode: string;
  customCriteria: string | null;
  instructions: string;
  summary: string | null;
  promptVersion: string;
  responseVersion: number;
  createdAt: string;
  finishedAt: string | null;
  resultSource?: string;
}
export interface DocumentReviewRequestDTO {
  requestId: string;
  resumeNum: number;
  letterNum: number;
  portfolioNum: number | null;
  reviewMode: string;
  customCriteria: string | null;
  instructions: string;
}
export interface DocumentSummaryDTO {
  documentNum: number;
  documentTitle: string;
  createdAt: string;
}
export interface EducationDTO {
  eduNum: number;
  resumeNum: number;
  schoolName: string;
  admissionDate: string | null;
  graduationDate: string | null;
  major: string | null;
  educationStatus: string;
}
export interface MainResumeJobInfoDTO {
  resumeNum: number;
  resumeTitle: string;
  occupationCode: string;
  occupationName: string;
  jobCode: string;
  jobName: string;
}
export interface MainResumeRequestDTO {
  resumeNum: number;
}
export interface PortfolioDTO {
  portfolioNum: number;
  userNum: number;
  portfolioTitle: string;
  originalFileName: string | null;
  createdAt: string;
  updatedAt: string;
}
export interface ResumeDetailDTO {
  resume: ResumeDTO;
  educationList: EducationDTO[];
  careerList: CareerDTO[];
  certificationList: CertificationDTO[];
}
export interface ResumeDTO {
  resumeNum: number;
  userNum: number;
  resumeTitle: string | null;
  occupationCode: string | null;
  occupationName: string | null;
  jobCode: string | null;
  jobName: string | null;
  highestLevel: string;
  motivation: string | null;
  desiredLocation: string | null;
  desiredWorkType: string | null;
  createdAt: string;
  updatedAt: string;
}
export interface FileUploadResponseDTO {
  targetId: number;
  originalFileName: string;
  savedFileName: string;
  fileUrl: string;
  fileSize: number;
  category: string;
  uploadedAt: string;
}
export interface TodayFortuneDTO {
  fortuneDate: string;
  userNickname: string;
  dayMaster: string;
  dayMasterElement: string;
  todayIlju: string;
  todayElement: string;
  tenGodsRelation: string;
  tenGodsMeaning: string;
  overallScore: number;
  overallSummary: string;
  advice: string;
  luckyJobName: string;
  luckyKeyword: string;
  luckyColor: string;
  luckyDirection: string;
  luckyNumber: number;
  sinsalName: string;
  sinsalAdvice: string;
  engineSource: string;
  recommendRecruitments: RecruitmentDTO[];
}
export interface InterviewAnswerRequestDTO {
  questionOrder: number;
  answer: string;
  timedOut: boolean;
}
export interface InterviewAnswerResponseDTO {
  completed: boolean;
  nextQuestion: InterviewQuestionDTO | null;
  result: InterviewResultResponseDTO | null;
  questionDeadline: number;
  expiresAt: number;
  serverTime: number;
}
export interface InterviewAvailabilityDTO {
  maintenance: boolean;
  serverTime: number;
  nextChangeAt: number;
}
export interface InterviewDocumentOptionDTO {
  documentNum: number;
  documentTitle: string;
}
export interface InterviewDocumentResponseDTO {
  portfolioList: InterviewDocumentOptionDTO[];
  coverLetterList: InterviewDocumentOptionDTO[];
  resumeList: InterviewDocumentOptionDTO[];
}
export interface InterviewHistoryDTO {
  historyNum: number;
  userNum: number;
  question1: string;
  answer1: string;
  question2: string;
  answer2: string;
  question3: string;
  answer3: string;
  question4: string;
  answer4: string;
  question5: string;
  answer5: string;
  interviewDate: string;
}
export interface InterviewQuestionAnswerDTO {
  questionOrder: number;
  questionType: string;
  question: string;
  answer: string;
  timedOut: boolean;
}
export interface InterviewQuestionDTO {
  questionOrder: number;
  questionType: string;
  questionText: string;
}
export interface InterviewRecordDTO {
  history: InterviewHistoryDTO;
  result: InterviewResultDTO;
}
export interface InterviewRecruitmentDTO {
  recruitmentNum: number;
  companyName: string;
  title: string;
  jobName: string;
  locationName: string;
  experienceLevel: string;
}
export interface InterviewResultDTO {
  historyNum: number;
  userNum: number;
  documentConsistencyScore: number;
  expertiseScore: number;
  problemSolvingScore: number;
  logicScore: number;
  communicationScore: number;
  totalScore: number;
  strengths: string;
  weaknesses: string;
  comparison: string;
  improvements: string;
  interviewDate: string;
}
export interface InterviewResultResponseDTO {
  historyNum: number;
  documentConsistencyScore: number;
  expertiseScore: number;
  problemSolvingScore: number;
  logicScore: number;
  communicationScore: number;
  totalScore: number;
  strengths: string;
  weaknesses: string;
  comparison: string;
  improvements: string;
}
export interface InterviewSessionResponseDTO {
  historyNum: number;
  settings: InterviewStartRequestDTO;
  questions: InterviewQuestionDTO[];
  answers: InterviewQuestionAnswerDTO[];
  completed: boolean;
  answerLocked: boolean;
  pendingAnswer: string;
  result: InterviewResultResponseDTO | null;
  answerTimeLimitSeconds: number;
  questionDeadline: number;
  expiresAt: number;
  serverTime: number;
}
export interface InterviewStartRequestDTO {
  requestId: string;
  resumeNum: number;
  portfolioNum: number;
  letterNum: number;
  interviewDifficulty: string;
  recruitment: InterviewRecruitmentDTO;
}
export interface InterviewStartResponseDTO {
  historyNum: number;
  questions: InterviewQuestionDTO[];
  answerTimeLimitSeconds: number;
  questionDeadline: number;
  expiresAt: number;
  serverTime: number;
}
export interface JobDTO {
  jobCode: string;
  occupationCode: string;
  jobName: string;
}
export interface JobScrapResponseDTO {
  recruitmentNum: number;
  userNum: number;
  isScrapped: boolean;
  message: string;
}
export interface OccupationDTO {
  occupationCode: string;
  occupationName: string;
}
export interface RecruitmentDTO {
  recruitmentNum: number;
  saraminJobId: string;
  companyName: string;
  title: string;
  jobUrl: string;
  locationName: string;
  jobName: string;
  experienceLevel: string;
  expirationDate: string | null;
  closeType: string;
  isActive: number;
  createdAt: string;
  isScrapped: boolean;
}
export interface RecruitmentSearchDTO {
  keyword: string;
  location: string;
  experienceLevel: string;
  occupationCode: string;
  userNum: number;
  scrapOnly: boolean;
  sortBy: string;
  page: number;
  size: number;
}
export interface UserRecommendResponseDTO {
  recommendType: string;
  targetJobName: string;
  userNickname: string;
  recruitments: RecruitmentDTO[];
}
export interface ApiResponse<T> {
  message?: string;
  responseCode: ResponseCode;
  data: T;
}
export interface PageResponse<T> {
  content: T[];
  currentPage: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}
export interface StudyDTO {
  studyNum: number;
  userNum: number;
  userNickname: string;
  studyName: string;
  studyExplain: string;
  studyCreateDate: string;
  maxMembers: number;
  memberCount: number;
  isJoined: boolean;
}
export interface UserJobPreferenceDTO {
  userNum: number;
  userNickname: string;
  jobCode: string;
  jobName: string;
  occupationCode: string;
  occupationName: string;
}

export interface ResponseCode { code: number; message: string }
export interface ReviewSource { documentType: string; section?: string; text: string; pageNumber?: number | null }
export interface ReviewStrength { title: string; reason?: string; suggestion?: string; sources: ReviewSource[] }
export interface ReviewImprovement { section?: string; title?: string; issue: string; original: string; suggestion: string; reason: string }
export interface ReviewFeedback { summary: string; strengths: ReviewStrength[]; improvements: ReviewImprovement[] }
export interface ReviewConsistency { type: string; title: string; recommendation: string; sources: ReviewSource[] }
export interface CareerPreparationDTO {
 occupationCode: string; occupationName: string; jobCode: string; jobName: string;
 summary: string; coverageNote: string;
 recommendations: {category: string; title: string; reason: string; action: string; sources?: ReviewSource[]}[];
}
export interface ReviewDocument { reviewDocumentNum: number; documentType: string; sourceDocumentNum: number; documentTitle: string; sourceUpdatedAt: string; originalFileName: string }
export interface DocumentReviewResultDTO extends DocumentReviewDTO {
 documents: ReviewDocument[]; strengths: ReviewStrength[]; documentReviews: Record<string, ReviewFeedback>;
 consistencyIssues: ReviewConsistency[]; careerPreparation: CareerPreparationDTO | null;
}
export interface InterviewSettings { resumeNum: string; portfolioNum: string; letterNum: string; difficulty: string }
export interface InterviewProgress { version?: number; historyNum: number; expiresAt: number; questionOrder: number; answer: string; answerLocked?: boolean }
export type StorageLike = Pick<Storage, 'getItem' | 'setItem' | 'removeItem'>;
export type DocumentData = ResumeDetailDTO | CoverLetterDTO | PortfolioDTO;
export interface ApiErrorData { message?: string; responseCode?: Partial<ResponseCode>; data?: string | {message?: string; maintenance?: boolean; restartRequired?: boolean; answerLocked?: boolean} }

export type QueryParams = Record<string, string | number | boolean | null | undefined>;
export interface ResumeInput { resume: Partial<ResumeDTO>; educationList: NullableDates<EducationDTO>[]; careerList: NullableDates<CareerDTO>[]; certificationList: NullableDates<CertificationDTO>[] }

export interface LoadState<T> { loading: boolean; error: string; results: T[] }
export interface ReviewResource<T> { key: string; data?: T; error?: string }
export interface ReviewSelection {
 documents: DocumentSummaryDTO[]; isLoading: boolean; error: string; reload: () => void;
 selectedDocumentNum: string; requestedDocumentNum: string;
 source: {document: DocumentData | null; isLoading: boolean; error: string; reload: () => void};
}
export type ReviewSelectionKey = 'resumeNum' | 'letterNum' | 'portfolioNum';
export interface ReviewTypeConfig { value: DocumentType; selectionKey: ReviewSelectionKey; required: boolean; label: string; description: string; hint: string }

export type NullableDates<T> = { [K in keyof T]?: T[K] | null };
