package com.provit.mapper;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.support.TransactionTemplate;

import com.provit.dto.auth.RefreshTokenDTO;
import com.provit.dto.auth.UserDTO;
import com.provit.dto.community.CommentDTO;
import com.provit.dto.community.PostDTO;
import com.provit.dto.community.PostSearchDTO;
import com.provit.dto.document.*;
import com.provit.dto.interview.*;
import com.provit.dto.recruitment.RecruitmentDTO;
import com.provit.dto.recruitment.RecruitmentSearchDTO;
import com.provit.dto.study.StudyDTO;
import com.provit.support.PostgresTestDatabase;

public class PostgresMapperIntegrationTest {
    private static final String USER = "com.provit.mapper.UserMapper.";
    private static final String RESUME = "com.provit.mapper.document.ResumeMapper.";
    private static final String POST = "com.provit.mapper.PostMapper.";
    private static final String JOB = "recruitment_mapper.";
    private static final String STUDY = "com.provit.dao.study.StudyDAO.";
    private PostgresTestDatabase db;
    private UserDTO owner;

    @Before
    public void setup() throws Exception {
        db = PostgresTestDatabase.open();
        db.jdbc.update("INSERT INTO T_OCCUPATION VALUES ('2','IT개발·데이터')");
        db.jdbc.update("INSERT INTO T_JOB VALUES ('84','2','백엔드/서버개발')");
        owner = user("owner");
        db.session.insert(USER + "insertUser", owner);
    }

    @After
    public void cleanup() throws Exception { if (db != null) db.close(); }

    @Test
    public void referenceSeedIsRepeatableAndSequencesFollowFixedCategoryIds() throws Exception {
        var seed = new EncodedResource(new ClassPathResource("sql_query/seed_saramin_jobs.sql"), java.nio.charset.StandardCharsets.UTF_8);
        ScriptUtils.executeSqlScript(db.connection, seed);
        ScriptUtils.executeSqlScript(db.connection, seed);
        assertEquals(21, count("T_OCCUPATION"));
        assertEquals(2178, count("T_JOB"));
        assertEquals(4, count("T_CATEGORY"));
        assertEquals(Long.valueOf(5), db.jdbc.queryForObject("SELECT nextval('seq_t_category')", Long.class));
        assertEquals(21, db.session.selectList(JOB + "selectOccupationList").size());
        assertFalse(db.session.selectList(JOB + "selectJobListByOccupation", "2").isEmpty());
    }

    @Test
    public void userKeysNullCodesAndActiveNicknameUniquenessArePreserved() {
        var second = user("second");
        second.setOccupationCode("");
        second.setJobCode("");
        db.session.insert(USER + "insertUser", second);
        assertTrue(second.getUserNum() > owner.getUserNum());
        UserDTO saved = db.session.selectOne(USER + "selectByUserNum", second.getUserNum());
        assertNull(saved.getOccupationCode());
        assertNull(saved.getJobCode());
        assertNotNull(saved.getUserRegisterDate());
        var duplicate = user("third");
        duplicate.setUserNickname(owner.getUserNickname());
        assertThrows(DataIntegrityViolationException.class, () -> db.session.insert(USER + "insertUser", duplicate));
        db.session.update(USER + "withdrawMyAccount", owner.getUserNum());
        db.session.insert(USER + "insertUser", duplicate);
        assertEquals(1, (int) db.session.selectOne(USER + "countByNickname", duplicate.getUserNickname()));
    }

    @Test
    public void resumeTextAndCalendarDatesRoundTripWithNullOptionalValues() {
        ResumeDTO resume = resume("가".repeat(200));
        resume.setMotivation("긴 문서 😀".repeat(2000));
        resume.setDesiredLocation("");
        db.session.insert(RESUME + "insertResume", resume);
        var education = new EducationDTO();
        education.setResumeNum(resume.getResumeNum());
        education.setSchoolName("한국대학교");
        education.setEducationStatus("졸업");
        education.setAdmissionDate(java.sql.Date.valueOf("2020-03-02"));
        db.session.insert(RESUME + "insertEducation", education);
        ResumeDTO read = db.session.selectOne(RESUME + "selectResume", Map.of("resumeNum", resume.getResumeNum(), "userNum", owner.getUserNum()));
        assertEquals(resume.getMotivation(), read.getMotivation());
        assertNull(read.getDesiredLocation());
        assertEquals("백엔드/서버개발", read.getJobName());
        List<EducationDTO> schools = db.session.selectList(RESUME + "selectEducationList", resume.getResumeNum());
        assertEquals("2020-03-02", new java.sql.Date(schools.get(0).getAdmissionDate().getTime()).toString());
        assertNull(schools.get(0).getGraduationDate());
        resume.setResumeTitle("수정 제목");
        db.session.update(RESUME + "updateResume", resume);
        assertEquals(1, db.session.selectList(RESUME + "selectResumeSummaryList", owner.getUserNum().intValue()).size());
    }

    @Test
    public void documentTitlesEnforce200CharactersWithoutTruncatingKoreanOrEmoji() {
        for (String title : List.of("한".repeat(200), "A".repeat(200), "가A😀".repeat(50))) {
            ResumeDTO resume = resume(title);
            db.session.insert(RESUME + "insertResume", resume);
            ResumeDTO read = db.session.selectOne(RESUME + "selectResume", Map.of("resumeNum", resume.getResumeNum(), "userNum", owner.getUserNum()));
            assertEquals(title, read.getResumeTitle());
        }
        assertThrows(DataIntegrityViolationException.class, () -> db.session.insert(RESUME + "insertResume", resume("한".repeat(201))));
        String letterNs = "com.provit.mapper.document.CoverLetterMapper.";
        var letter = new CoverLetterDTO();
        letter.setUserNum(owner.getUserNum().intValue());
        letter.setCoverLetterTitle("자".repeat(200));
        db.session.insert(letterNs + "insertCoverLetter", letter);
        letter.setCoverLetterTitle("자".repeat(201));
        assertThrows(DataIntegrityViolationException.class, () -> db.session.insert(letterNs + "insertCoverLetter", letter));
        String portfolioNs = "com.provit.mapper.document.PortfolioMapper.";
        var portfolio = new PortfolioDTO();
        portfolio.setUserNum(owner.getUserNum().intValue());
        portfolio.setPortfolioNum(db.session.selectOne(portfolioNs + "selectNextPortfolioNum"));
        portfolio.setPortfolioTitle("포".repeat(200));
        db.session.insert(portfolioNs + "insertPortfolio", portfolio);
        portfolio.setPortfolioNum(db.session.selectOne(portfolioNs + "selectNextPortfolioNum"));
        portfolio.setPortfolioTitle("포".repeat(201));
        assertThrows(DataIntegrityViolationException.class, () -> db.session.insert(portfolioNs + "insertPortfolio", portfolio));
    }

    @Test
    public void postPagingSearchPopularAndByteBoundaryKeepTheirResults() {
        PostDTO first = post("한글 검색", "가".repeat(1333) + "A");
        db.session.insert(POST + "insertPost", first);
        db.session.insert(POST + "insertPost", post("두 번째", "내용"));
        var search = new PostSearchDTO();
        search.setKeyword("한글");
        assertEquals(1, (int) db.session.selectOne(POST + "countPosts", search));
        List<PostDTO> found = db.session.selectList(POST + "selectPostList", search);
        assertEquals(first.getPostNum(), found.get(0).getPostNum());
        assertEquals(1, db.session.selectList(POST + "selectPopularPosts", 1).size());
        assertThrows(DataIntegrityViolationException.class, () -> db.session.insert(POST + "insertPost", post("초과", "가".repeat(1333) + "AA")));
        assertEquals(2, count("T_POST"));
    }

    @Test
    public void commentsRepliesLikesAndFileOwnershipQueriesUseExistingKeys() {
        PostDTO post = post("댓글 테스트", "본문");
        db.session.insert(POST + "insertPost", post);
        var comment = new CommentDTO();
        comment.setPostNum(post.getPostNum());
        comment.setUserNum(owner.getUserNum());
        comment.setCommentContent("원댓글");
        String ns = "com.provit.dao.community.CommentDAO.";
        db.session.insert(ns + "insertComment", comment);
        var reply = new CommentDTO();
        reply.setPostNum(post.getPostNum());
        reply.setUserNum(owner.getUserNum());
        reply.setCommentContent("답글");
        reply.setParentCommentNum(comment.getCommentNum());
        db.session.insert(ns + "insertComment", reply);
        List<CommentDTO> comments = db.session.selectList(ns + "selectCommentList", post.getPostNum());
        assertEquals(comment.getCommentNum(), comments.get(0).getCommentNum());
        assertEquals(reply.getCommentNum(), comments.get(1).getCommentNum());
        var pair = Map.of("postNum", post.getPostNum(), "userNum", owner.getUserNum());
        db.session.insert(POST + "insertPostLike", pair);
        assertEquals(1, (int) db.session.selectOne(POST + "checkPostLike", pair));
        assertEquals(1, db.session.delete(POST + "deletePostLike", pair));
        assertEquals(owner.getUserNum().intValue(), (int) db.session.selectOne("com.provit.mapper.file.FileMapper.selectPostOwner", post.getPostNum()));
    }

    @Test
    public void recruitmentUpsertDoesNotDuplicateAndScrapConflictKeepsTransactionUsable() {
        RecruitmentDTO recruitment = recruitment("external-1");
        db.session.insert(JOB + "mergeRecruitment", recruitment);
        recruitment.setTitle("수정된 백엔드 공고");
        recruitment.setIsActive(null);
        db.session.insert(JOB + "mergeRecruitment", recruitment);
        assertEquals(1, count("T_RECRUITMENT"));
        long number = db.jdbc.queryForObject("SELECT RECRUITMENT_NUM FROM T_RECRUITMENT", Long.class);
        var pair = Map.of("recruitmentNum", number, "userNum", owner.getUserNum());
        new TransactionTemplate(db.transactions).executeWithoutResult(status -> {
            assertEquals(1, db.session.insert(JOB + "insertJobScrap", pair));
            assertEquals(0, db.session.insert(JOB + "insertJobScrap", pair));
            assertEquals(1, (int) db.session.selectOne(JOB + "checkJobScrap", pair));
        });
        var search = RecruitmentSearchDTO.builder().userNum(owner.getUserNum()).scrapOnly(true)
                .sortBy("CLOSING_SOON").keyword("백엔드").build();
        List<RecruitmentDTO> results = db.session.selectList(JOB + "selectRecruitmentList", search);
        assertEquals(1, results.size());
        assertTrue(results.get(0).getIsScrapped());
        assertEquals(1L, (long) db.session.selectOne(JOB + "selectRecruitmentCount", search));
        assertFalse(db.session.selectList(JOB + "selectRecruitmentsByKeyword", Map.of("keyword", "백엔드", "userNum", owner.getUserNum(), "limit", 3)).isEmpty());
        assertFalse(db.session.selectList(JOB + "selectHotRecruitments", Map.of("userNum", owner.getUserNum(), "limit", 3)).isEmpty());
    }

    @Test
    public void recruitmentDateIntervalsExpireAndPurgeOnlyEligibleRows() {
        db.session.insert(JOB + "mergeRecruitment", recruitment("old"));
        db.session.insert(JOB + "mergeRecruitment", recruitment("active"));
        db.jdbc.update("UPDATE T_RECRUITMENT SET CREATED_AT=LOCALTIMESTAMP - INTERVAL '200 days' WHERE SARAMIN_JOB_ID='old'");
        assertEquals(1, db.session.update(JOB + "deactivateExpiredRecruitments"));
        assertEquals(1, db.session.delete(JOB + "purgeOldUnscrappedRecruitments"));
        assertEquals(1, count("T_RECRUITMENT"));
    }

    @Test
    public void studyMembershipAndForUpdateSubqueryWorkInsideTransaction() {
        var study = new StudyDTO();
        study.setUserNum(owner.getUserNum());
        study.setStudyName("PostgreSQL 스터디");
        study.setMaxMembers(2);
        db.session.insert(STUDY + "insertStudy", study);
        db.session.insert(STUDY + "insertStudyMember", Map.of("studyNum", study.getStudyNum(), "userNum", owner.getUserNum()));
        List<StudyDTO> anonymous = db.session.selectList(STUDY + "selectStudyList", null);
        assertFalse(anonymous.get(0).getIsJoined());
        List<StudyDTO> joined = db.session.selectList(STUDY + "selectStudyList", owner.getUserNum());
        assertTrue(joined.get(0).getIsJoined());
        new TransactionTemplate(db.transactions).executeWithoutResult(status -> {
            StudyDTO locked = db.session.selectOne(STUDY + "selectStudyDetailForUpdate", study.getStudyNum());
            assertEquals(1, locked.getMemberCount());
        });
        assertEquals(1, db.session.selectList(STUDY + "selectStudyMembers", study.getStudyNum()).size());
    }

    @Test
    public void interviewLengthBoundariesScoresAndTimeOfDayRoundTrip() {
        String ns = "com.provit.mapper.interview.InterviewHistoryMapper.";
        var history = new InterviewHistoryDTO();
        history.setHistoryNum(db.session.selectOne(ns + "selectNextHistoryNum"));
        history.setUserNum(owner.getUserNum().intValue());
        history.setQuestion1("질".repeat(120));
        history.setAnswer1("답".repeat(1000));
        Date at = java.sql.Timestamp.valueOf("2026-10-02 14:35:27");
        history.setInterviewDate(at);
        db.session.insert(ns + "insertInterviewHistory", history);
        var key = Map.of("number", history.getHistoryNum(), "userNum", owner.getUserNum());
        InterviewHistoryDTO read = db.session.selectOne(ns + "selectInterviewHistory", key);
        assertEquals(at.getTime(), read.getInterviewDate().getTime());
        assertEquals(history.getAnswer1(), read.getAnswer1());
        history.setHistoryNum(db.session.selectOne(ns + "selectNextHistoryNum"));
        history.setQuestion1("질".repeat(121));
        assertThrows(DataIntegrityViolationException.class, () -> db.session.insert(ns + "insertInterviewHistory", history));
        history.setQuestion1("질문"); history.setAnswer1("답".repeat(1001));
        assertThrows(DataIntegrityViolationException.class, () -> db.session.insert(ns + "insertInterviewHistory", history));
        var result = new InterviewResultDTO();
        result.setHistoryNum(read.getHistoryNum()); result.setUserNum(owner.getUserNum().intValue());
        result.setStrengths("강".repeat(250)); result.setExpertiseScore(87.25); result.setInterviewDate(at);
        String results = "com.provit.mapper.interview.InterviewResultMapper.";
        db.session.insert(results + "insertInterviewResult", result);
        InterviewResultDTO scored = db.session.selectOne(results + "selectInterviewResult", key);
        assertEquals(87.25, scored.getExpertiseScore(), 0.001);
        assertEquals(at.getTime(), scored.getInterviewDate().getTime());
        result.setStrengths("강".repeat(251));
        assertThrows(DataIntegrityViolationException.class, () -> db.session.insert(results + "insertInterviewResult", result));
    }

    @Test
    public void refreshExpiryAndAdminBlockingUsePostgresqlTimestamps() {
        String ns = "com.provit.mapper.RefreshTokenMapper.";
        var token = new RefreshTokenDTO();
        token.setTokenId("expired"); token.setUserNum(owner.getUserNum()); token.setExpiresAt(new Date(1));
        db.session.insert(ns + "insert", token);
        assertNotNull(db.session.selectOne(ns + "selectByTokenId", "expired"));
        assertEquals(1, db.session.delete(ns + "deleteExpired"));
        String admin = "com.provit.mapper.AdminUserMapper.";
        db.session.update(admin + "updateBlockedDate", Map.of("userNum", owner.getUserNum(), "blockDays", 7));
        var search = Map.of("status", "BLOCKED", "offset", 0, "limit", 10);
        assertEquals(1, db.session.selectList(admin + "selectUserList", search).size());
        assertEquals(1, (int) db.session.selectOne(admin + "selectUserCount", search));
        db.session.update(admin + "updateBlockedDate", Map.of("userNum", owner.getUserNum(), "blockDays", 9999));
        db.session.update(admin + "updateBlockedDate", Map.of("userNum", owner.getUserNum(), "blockDays", 0));
        assertEquals(0, (int) db.session.selectOne(admin + "selectUserCount", search));
        assertNotNull(db.session.selectOne("com.provit.mapper.SampleMapper.selectCurrentTime"));
    }

    @Test
    public void reportsKeepPreviewFallbackAndBlockFurtherPostUpdates() {
        PostDTO post = post("신고 대상", "내용");
        db.session.insert(POST + "insertPost", post);
        var report = Map.of("reporterNum", owner.getUserNum(), "targetType", "POST", "targetNum", post.getPostNum(), "reportReason", "부적절한 내용");
        db.session.insert("com.provit.mapper.ReportMapper.insertReport", report);
        assertEquals(1, (int) db.session.selectOne("com.provit.mapper.ReportMapper.checkDuplicateReport", report));
        String admin = "com.provit.mapper.AdminReportMapper.";
        assertEquals(1, db.session.selectList(admin + "selectReportList", Map.of("offset", 0, "limit", 10)).size());
        long reportNum = db.jdbc.queryForObject("SELECT REPORT_NUM FROM T_REPORT", Long.class);
        db.session.update(admin + "updateReportStatus", Map.of("reportNum", reportNum, "status", "RESOLVED"));
        db.session.update(admin + "blindPost", post.getPostNum());
        assertEquals(0, db.session.update(POST + "updatePost", post));
        assertEquals(owner.getUserEmail(), db.session.selectOne(admin + "getTargetUserEmail", Map.of("targetType", "POST", "targetNum", post.getPostNum())));
    }

    private int count(String table) { return db.jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    private UserDTO user(String name) {
        return UserDTO.builder().userName(name).userNickname(name).userEmail(name + "@example.invalid")
                .userPw("placeholder").userType("USER").userIsDeleted(0).build();
    }
    private ResumeDTO resume(String title) {
        var resume = new ResumeDTO(); resume.setUserNum(owner.getUserNum().intValue());
        resume.setResumeTitle(title); resume.setHighestLevel("대학교"); resume.setOccupationCode("2"); resume.setJobCode("84");
        return resume;
    }
    private PostDTO post(String title, String content) {
        var post = new PostDTO(); post.setUserNum(owner.getUserNum()); post.setCategoryNum(1);
        post.setPostTitle(title); post.setPostContent(content); return post;
    }
    private RecruitmentDTO recruitment(String id) {
        return RecruitmentDTO.builder().saraminJobId(id).companyName("회사").title("백엔드 공고")
                .jobName("백엔드/서버개발").jobUrl("https://example.invalid/job/" + id).isActive(1).build();
    }
}
