package com.provit.mapper;

import static org.junit.Assert.*;

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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class PostgresJpaIntegrationTest {
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
        db.users.insertUser(owner);
    }

    @After
    public void cleanup() throws Exception {
        if (db != null) db.close();
    }

    @Test
    public void referenceSeedIsRepeatableAndSequencesFollowFixedCategoryIds() throws Exception {
        var seed =
                new EncodedResource(
                        new ClassPathResource("sql_query/seed_saramin_jobs.sql"),
                        java.nio.charset.StandardCharsets.UTF_8);
        ScriptUtils.executeSqlScript(db.connection, seed);
        ScriptUtils.executeSqlScript(db.connection, seed);
        assertEquals(21, count("T_OCCUPATION"));
        assertEquals(2178, count("T_JOB"));
        assertEquals(4, count("T_CATEGORY"));
        assertEquals(
                Long.valueOf(5),
                db.jdbc.queryForObject("SELECT nextval('seq_t_category')", Long.class));
        assertEquals(21, db.recruitment.selectOccupationList().size());
        assertFalse(db.recruitment.selectJobListByOccupation("2").isEmpty());
    }

    @Test
    public void userKeysNullCodesAndActiveNicknameUniquenessArePreserved() {
        var second = user("second");
        second.setOccupationCode("");
        second.setJobCode("");
        db.users.insertUser(second);
        assertTrue(second.getUserNum() > owner.getUserNum());
        UserDTO saved = db.users.selectByUserNum(second.getUserNum());
        assertNull(saved.getOccupationCode());
        assertNull(saved.getJobCode());
        assertNotNull(saved.getUserRegisterDate());
        var duplicate = user("third");
        duplicate.setUserNickname(owner.getUserNickname());
        assertThrows(DataIntegrityViolationException.class, () -> db.users.insertUser(duplicate));
        db.users.withdrawMyAccount(owner.getUserNum());
        db.users.insertUser(duplicate);
        assertEquals(1, (int) db.users.countByNickname(duplicate.getUserNickname()));
    }

    @Test
    public void resumeTextAndCalendarDatesRoundTripWithNullOptionalValues() {
        ResumeDTO resume = resume("가".repeat(200));
        resume.setMotivation("긴 문서 😀".repeat(2000));
        resume.setDesiredLocation("");
        db.documents.insertResume(resume);
        var education = new EducationDTO();
        education.setResumeNum(resume.getResumeNum());
        education.setSchoolName("한국대학교");
        education.setEducationStatus("졸업");
        education.setAdmissionDate(java.sql.Date.valueOf("2020-03-02"));
        db.documents.insertEducation(education);
        ResumeDTO read =
                db.documents.selectResume(owner.getUserNum().intValue(), resume.getResumeNum());
        assertEquals(resume.getMotivation(), read.getMotivation());
        assertNull(read.getDesiredLocation());
        assertEquals("백엔드/서버개발", read.getJobName());
        List<EducationDTO> schools = db.documents.selectEducationList(resume.getResumeNum());
        assertEquals(
                "2020-03-02",
                new java.sql.Date(schools.get(0).getAdmissionDate().getTime()).toString());
        assertNull(schools.get(0).getGraduationDate());
        resume.setResumeTitle("수정 제목");
        db.documents.updateResume(resume);
        assertEquals(1, db.documents.selectResumeSummaryList(owner.getUserNum().intValue()).size());
    }

    @Test
    public void documentTitlesEnforce200CharactersWithoutTruncatingKoreanOrEmoji() {
        for (String title : List.of("한".repeat(200), "A".repeat(200), "가A😀".repeat(50))) {
            ResumeDTO resume = resume(title);
            db.documents.insertResume(resume);
            ResumeDTO read =
                    db.documents.selectResume(owner.getUserNum().intValue(), resume.getResumeNum());
            assertEquals(title, read.getResumeTitle());
        }
        assertThrows(
                DataIntegrityViolationException.class,
                () -> db.documents.insertResume(resume("한".repeat(201))));
        String letterNs = "com.provit.mapper.document.CoverLetterMapper.";
        var letter = new CoverLetterDTO();
        letter.setUserNum(owner.getUserNum().intValue());
        letter.setCoverLetterTitle("자".repeat(200));
        db.documents.insertCoverLetter(letter);
        letter.setCoverLetterTitle("자".repeat(201));
        assertThrows(
                DataIntegrityViolationException.class,
                () -> db.documents.insertCoverLetter(letter));
        String portfolioNs = "com.provit.mapper.document.PortfolioMapper.";
        var portfolio = new PortfolioDTO();
        portfolio.setUserNum(owner.getUserNum().intValue());
        portfolio.setPortfolioNum(db.documents.selectNextPortfolioNum());
        portfolio.setPortfolioTitle("포".repeat(200));
        db.documents.insertPortfolio(portfolio);
        portfolio.setPortfolioNum(db.documents.selectNextPortfolioNum());
        portfolio.setPortfolioTitle("포".repeat(201));
        assertThrows(
                DataIntegrityViolationException.class,
                () -> db.documents.insertPortfolio(portfolio));
    }

    @Test
    public void postPagingSearchPopularAndByteBoundaryKeepTheirResults() {
        PostDTO first = post("한글 검색", "가".repeat(1333) + "A");
        db.posts.insertPost(first);
        db.posts.insertPost(post("두 번째", "내용"));
        var search = new PostSearchDTO();
        search.setKeyword("한글");
        assertEquals(1, (int) db.posts.countPosts(search));
        List<PostDTO> found = db.posts.selectPostList(search);
        assertEquals(first.getPostNum(), found.get(0).getPostNum());
        assertEquals(1, db.posts.selectPopularPosts(1).size());
        assertThrows(
                DataIntegrityViolationException.class,
                () -> db.posts.insertPost(post("초과", "가".repeat(1333) + "AA")));
        assertEquals(2, count("T_POST"));
    }

    @Test
    public void commentsRepliesLikesAndFileOwnershipQueriesUseExistingKeys() {
        PostDTO post = post("댓글 테스트", "본문");
        db.posts.insertPost(post);
        var comment = new CommentDTO();
        comment.setPostNum(post.getPostNum());
        comment.setUserNum(owner.getUserNum());
        comment.setCommentContent("원댓글");
        String ns = "com.provit.dao.community.CommentDAO.";
        db.comments.insertComment(comment);
        var reply = new CommentDTO();
        reply.setPostNum(post.getPostNum());
        reply.setUserNum(owner.getUserNum());
        reply.setCommentContent("답글");
        reply.setParentCommentNum(comment.getCommentNum());
        db.comments.insertComment(reply);
        List<CommentDTO> comments = db.comments.selectCommentList(post.getPostNum());
        assertEquals(comment.getCommentNum(), comments.get(0).getCommentNum());
        assertEquals(reply.getCommentNum(), comments.get(1).getCommentNum());
        Map<String, Object> pair =
                Map.of("postNum", post.getPostNum(), "userNum", owner.getUserNum());
        db.posts.insertPostLike(pair);
        assertEquals(1, (int) db.posts.checkPostLike(pair));
        assertEquals(1, db.posts.deletePostLike(pair));
        assertEquals(
                owner.getUserNum().intValue(), (int) db.fileOwners.postOwner(post.getPostNum()));
    }

    @Test
    public void recruitmentUpsertDoesNotDuplicateAndScrapConflictKeepsTransactionUsable() {
        RecruitmentDTO recruitment = recruitment("external-1");
        db.recruitment.mergeRecruitment(recruitment);
        recruitment.setTitle("수정된 백엔드 공고");
        recruitment.setIsActive(null);
        db.recruitment.mergeRecruitment(recruitment);
        assertEquals(1, count("T_RECRUITMENT"));
        long number =
                db.jdbc.queryForObject("SELECT RECRUITMENT_NUM FROM T_RECRUITMENT", Long.class);
        var pair = Map.of("recruitmentNum", number, "userNum", owner.getUserNum());
        new TransactionTemplate(db.transactions)
                .executeWithoutResult(
                        status -> {
                            assertEquals(
                                    1,
                                    db.recruitment.insertJobScrap(
                                            ((Number) pair.get("recruitmentNum")).longValue(),
                                            ((Number) pair.get("userNum")).longValue()));
                            assertEquals(
                                    0,
                                    db.recruitment.insertJobScrap(
                                            ((Number) pair.get("recruitmentNum")).longValue(),
                                            ((Number) pair.get("userNum")).longValue()));
                            assertEquals(
                                    1,
                                    (int)
                                            db.recruitment.checkJobScrap(
                                                    ((Number) pair.get("recruitmentNum"))
                                                            .longValue(),
                                                    ((Number) pair.get("userNum")).longValue()));
                        });
        var search =
                RecruitmentSearchDTO.builder()
                        .userNum(owner.getUserNum())
                        .scrapOnly(true)
                        .sortBy("CLOSING_SOON")
                        .keyword("백엔드")
                        .build();
        List<RecruitmentDTO> results = db.recruitment.selectRecruitmentList(search);
        assertEquals(1, results.size());
        assertTrue(results.get(0).getIsScrapped());
        assertEquals(1L, (long) db.recruitment.selectRecruitmentCount(search));
        assertFalse(
                db.recruitment.selectRecruitmentsByKeyword("백엔드", owner.getUserNum(), 3).isEmpty());
        assertFalse(db.recruitment.selectHotRecruitments(owner.getUserNum(), 3).isEmpty());
    }

    @Test
    public void recruitmentDateIntervalsExpireAndPurgeOnlyEligibleRows() {
        db.recruitment.mergeRecruitment(recruitment("old"));
        db.recruitment.mergeRecruitment(recruitment("active"));
        db.jdbc.update(
                "UPDATE T_RECRUITMENT SET CREATED_AT=LOCALTIMESTAMP - INTERVAL '200 days' WHERE"
                        + " SARAMIN_JOB_ID='old'");
        assertEquals(1, db.recruitment.deactivateExpiredRecruitments());
        assertEquals(1, db.recruitment.purgeOldUnscrappedRecruitments());
        assertEquals(1, count("T_RECRUITMENT"));
    }

    @Test
    public void studyMembershipAndForUpdateSubqueryWorkInsideTransaction() {
        var study = new StudyDTO();
        study.setUserNum(owner.getUserNum());
        study.setStudyName("PostgreSQL 스터디");
        study.setMaxMembers(2);
        db.studies.insertStudy(study);
        db.studies.insertStudyMember(
                Map.of("studyNum", study.getStudyNum(), "userNum", owner.getUserNum()));
        List<StudyDTO> anonymous = db.studies.selectStudyList(null);
        assertFalse(anonymous.get(0).getIsJoined());
        List<StudyDTO> joined = db.studies.selectStudyList(owner.getUserNum());
        assertTrue(joined.get(0).getIsJoined());
        new TransactionTemplate(db.transactions)
                .executeWithoutResult(
                        status -> {
                            StudyDTO locked =
                                    db.studies.selectStudyDetailForUpdate(study.getStudyNum());
                            assertEquals(1, locked.getMemberCount());
                        });
        assertEquals(1, db.studies.selectStudyMembers(study.getStudyNum()).size());
    }

    @Test
    public void interviewLengthBoundariesScoresAndTimeOfDayRoundTrip() {
        String ns = "com.provit.mapper.interview.InterviewHistoryMapper.";
        var history = new InterviewHistoryDTO();
        history.setHistoryNum(db.interview.selectNextHistoryNum());
        history.setUserNum(owner.getUserNum().intValue());
        history.setQuestion1("질".repeat(120));
        history.setAnswer1("답".repeat(1000));
        Date at = java.sql.Timestamp.valueOf("2026-10-02 14:35:27");
        history.setInterviewDate(at);
        db.interview.insertInterviewHistory(history);
        var key = Map.of("number", history.getHistoryNum(), "userNum", owner.getUserNum());
        InterviewHistoryDTO read =
                db.interview.selectInterviewHistory(
                        ((Number) key.get("number")).intValue(),
                        ((Number) key.get("userNum")).intValue());
        assertEquals(at.getTime(), read.getInterviewDate().getTime());
        assertEquals(history.getAnswer1(), read.getAnswer1());
        history.setHistoryNum(db.interview.selectNextHistoryNum());
        history.setQuestion1("질".repeat(121));
        assertThrows(
                DataIntegrityViolationException.class,
                () -> db.interview.insertInterviewHistory(history));
        history.setQuestion1("질문");
        history.setAnswer1("답".repeat(1001));
        assertThrows(
                DataIntegrityViolationException.class,
                () -> db.interview.insertInterviewHistory(history));
        var result = new InterviewResultDTO();
        result.setHistoryNum(read.getHistoryNum());
        result.setUserNum(owner.getUserNum().intValue());
        result.setStrengths("강".repeat(250));
        result.setExpertiseScore(87.25);
        result.setInterviewDate(at);
        String results = "com.provit.mapper.interview.InterviewResultMapper.";
        db.interview.insertInterviewResult(result);
        InterviewResultDTO scored =
                db.interview.selectInterviewResult(
                        ((Number) key.get("number")).intValue(),
                        ((Number) key.get("userNum")).intValue());
        assertEquals(87.25, scored.getExpertiseScore(), 0.001);
        assertEquals(at.getTime(), scored.getInterviewDate().getTime());
        result.setStrengths("강".repeat(251));
        assertThrows(
                DataIntegrityViolationException.class,
                () -> db.interview.insertInterviewResult(result));
    }

    @Test
    public void refreshExpiryAndAdminBlockingUsePostgresqlTimestamps() {
        String ns = "com.provit.mapper.RefreshTokenMapper.";
        var token = new RefreshTokenDTO();
        token.setTokenId("expired");
        token.setUserNum(owner.getUserNum());
        token.setExpiresAt(new Date(1));
        db.refreshTokens.insert(token);
        assertNotNull(db.refreshTokens.selectByTokenId("expired"));
        assertEquals(1, db.refreshTokens.deleteExpired());
        String admin = "com.provit.mapper.AdminUserMapper.";
        db.adminUsers.updateBlockedDate(Map.of("userNum", owner.getUserNum(), "blockDays", 7));
        Map<String, Object> search = Map.of("status", "BLOCKED", "offset", 0, "limit", 10);
        assertEquals(1, db.adminUsers.selectUserList(search).size());
        assertEquals(1, (int) db.adminUsers.selectUserCount(search));
        db.adminUsers.updateBlockedDate(Map.of("userNum", owner.getUserNum(), "blockDays", 9999));
        db.adminUsers.updateBlockedDate(Map.of("userNum", owner.getUserNum(), "blockDays", 0));
        assertEquals(0, (int) db.adminUsers.selectUserCount(search));
        assertNotNull(db.health.selectCurrentTime());
    }

    @Test
    public void reportsKeepPreviewFallbackAndBlockFurtherPostUpdates() throws Exception {
        PostDTO post = post("신고 대상", "내용");
        db.posts.insertPost(post);
        var report =
                Map.of(
                        "reporterNum",
                        owner.getUserNum(),
                        "targetType",
                        "POST",
                        "targetNum",
                        post.getPostNum(),
                        "reportReason",
                        "부적절한 내용");
        db.reports.insertReport(report(report));
        assertEquals(1, (int) db.reports.checkDuplicateReport(report(report)));
        String admin = "com.provit.mapper.AdminReportMapper.";
        assertEquals(1, db.adminReports.selectReportList(Map.of("offset", 0, "limit", 10)).size());
        long reportNum = db.jdbc.queryForObject("SELECT REPORT_NUM FROM T_REPORT", Long.class);
        db.adminReports.updateReportStatus(Map.of("reportNum", reportNum, "status", "RESOLVED"));
        db.adminReports.blindPost(post.getPostNum());
        assertEquals(0, db.posts.updatePost(post));
        assertEquals(
                owner.getUserEmail(),
                db.adminReports.getTargetUserEmail("POST", post.getPostNum()));
    }

    private static com.provit.dto.community.ReportDTO report(Map<String, ?> values) {
        return com.provit.persistence.DtoMapper.copy(
                values, new com.provit.dto.community.ReportDTO(), Map.of());
    }

    @Test
    public void profileChangesDoNotOverwriteStoredOccupationOrJob() {
        var member = user("profile-member");
        member.setOccupationCode("2");
        member.setJobCode("84");
        db.users.insertUser(member);
        member.setUserNickname("새 닉네임");
        member.setOccupationCode("untrusted");
        member.setJobCode("untrusted");
        assertEquals(1, db.users.updateMyProfile(member));
        var saved = db.users.selectByUserNum(member.getUserNum());
        assertEquals("새 닉네임", saved.getUserNickname());
        assertEquals("2", saved.getOccupationCode());
        assertEquals("84", saved.getJobCode());
    }

    @Test
    public void savingAnUnchangedDocumentStillUpdatesItsModifiedTime() {
        var resume = resume("같은 내용 저장");
        db.documents.insertResume(resume);
        db.jdbc.update(
                "UPDATE T_RESUME SET UPDATED_AT=TIMESTAMP '2000-01-01 00:00:00' WHERE RESUME_NUM=?",
                resume.getResumeNum());
        db.documents.updateResume(resume);
        assertTrue(
                db.documents
                                .selectResume(owner.getUserNum().intValue(), resume.getResumeNum())
                                .getUpdatedAt()
                                .getTime()
                        > java.sql.Timestamp.valueOf("2000-01-01 00:00:00").getTime());
    }

    @Test
    public void interviewServiceRollsBackHistoryWhenResultPersistenceFails() {
        var target = new com.provit.service.interview.InterviewPersistenceService(db.interview);
        var proxy = new org.springframework.aop.framework.ProxyFactory(target);
        proxy.addAdvice(
                new org.springframework.transaction.interceptor.TransactionInterceptor(
                        db.transactions,
                        new org.springframework.transaction.annotation
                                .AnnotationTransactionAttributeSource()));
        var persistence =
                (com.provit.service.interview.InterviewPersistenceService) proxy.getProxy();
        var history = new InterviewHistoryDTO();
        history.setUserNum(owner.getUserNum().intValue());
        history.setQuestion1("질문");
        var result = new InterviewResultDTO();
        result.setStrengths("초".repeat(251));
        assertThrows(
                DataIntegrityViolationException.class, () -> persistence.save(history, result));
        assertEquals(0, count("T_INTERVIEW_HISTORY"));
        assertEquals(0, count("T_INTERVIEW_RESULT"));
    }

    @Test
    public void insertsIgnoreClientSuppliedCountersDeletionFlagsAndServerDates() {
        var member = user("server-fields");
        member.setUserIsDeleted(1);
        member.setUserTokenVersion(99);
        member.setBlockedDate(new Date(System.currentTimeMillis() + 86_400_000));
        member.setUserRegisterDate(new Date(0));
        db.users.insertUser(member);
        var saved = db.users.selectByUserNum(member.getUserNum());
        assertEquals(Integer.valueOf(0), saved.getUserIsDeleted());
        assertEquals(Integer.valueOf(0), saved.getUserTokenVersion());
        assertNull(saved.getBlockedDate());
        assertTrue(saved.getUserRegisterDate().getTime() > 0);
        var post = post("서버 소유 필드", "본문");
        post.setPostLikeCount(999);
        post.setViewCount(888);
        post.setPostDate(new Date(0));
        db.posts.insertPost(post);
        var storedPost = db.posts.selectPostDetail(post.getPostNum());
        assertEquals(Integer.valueOf(0), storedPost.getPostLikeCount());
        assertEquals(Integer.valueOf(0), storedPost.getViewCount());
        assertTrue(storedPost.getPostDate().getTime() > 0);
        var comment = new CommentDTO();
        comment.setPostNum(post.getPostNum());
        comment.setUserNum(owner.getUserNum());
        comment.setCommentContent("새 댓글");
        comment.setIsDeleted(1);
        comment.setCommentDate("forged-date");
        db.comments.insertComment(comment);
        assertEquals(
                Integer.valueOf(0),
                db.comments.selectCommentDetail(comment.getCommentNum()).getIsDeleted());
    }

    @Test
    public void bulkUpdatesFlushPendingChangesClearStaleEntitiesAndRollbackTogether() {
        new TransactionTemplate(db.transactions)
                .executeWithoutResult(
                        status -> {
                            var entity =
                                    db.store
                                            .entities(
                                                    com.provit.persistence.entity.UserEntity.class,
                                                    "where e.userNum=:id",
                                                    com.provit.persistence.JpaStore.params(
                                                            "id", owner.getUserNum()),
                                                    0,
                                                    1)
                                            .get(0);
                            entity.setUserNickname("트랜잭션 안의 변경");
                            db.users.incrementTokenVersion(owner.getUserNum());
                            var current = db.users.selectByUserNum(owner.getUserNum());
                            assertEquals("트랜잭션 안의 변경", current.getUserNickname());
                            assertEquals(Integer.valueOf(1), current.getUserTokenVersion());
                            status.setRollbackOnly();
                        });
        var restored = db.users.selectByUserNum(owner.getUserNum());
        assertEquals(owner.getUserNickname(), restored.getUserNickname());
        assertEquals(Integer.valueOf(0), restored.getUserTokenVersion());
    }

    @Test
    public void pagedListsAndDocumentSummariesHaveBoundedQueryCounts() {
        for (int index = 0; index < 5; index++) {
            db.posts.insertPost(post("페이지 " + index, "내용"));
            db.documents.insertResume(resume("이력서 " + index));
        }
        var statistics =
                db.entityManagerFactory()
                        .unwrap(org.hibernate.SessionFactory.class)
                        .getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        var query = new PostSearchDTO();
        query.setPage(2);
        query.setPageSize(2);
        var page = db.posts.selectPostList(query);
        assertEquals(2, page.size());
        assertEquals("페이지 2", page.get(0).getPostTitle());
        assertEquals(5, db.posts.countPosts(query));
        assertEquals(2L, statistics.getPrepareStatementCount());
        statistics.clear();
        assertEquals(5, db.documents.selectResumeSummaryList(owner.getUserNum().intValue()).size());
        assertEquals(1L, statistics.getPrepareStatementCount());
    }

    private int count(String table) {
        return db.jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private UserDTO user(String name) {
        return UserDTO.builder()
                .userName(name)
                .userNickname(name)
                .userEmail(name + "@example.invalid")
                .userPw("placeholder")
                .userType("USER")
                .userIsDeleted(0)
                .build();
    }

    private ResumeDTO resume(String title) {
        var resume = new ResumeDTO();
        resume.setUserNum(owner.getUserNum().intValue());
        resume.setResumeTitle(title);
        resume.setHighestLevel("대학교");
        resume.setOccupationCode("2");
        resume.setJobCode("84");
        return resume;
    }

    private PostDTO post(String title, String content) {
        var post = new PostDTO();
        post.setUserNum(owner.getUserNum());
        post.setCategoryNum(1);
        post.setPostTitle(title);
        post.setPostContent(content);
        return post;
    }

    private RecruitmentDTO recruitment(String id) {
        return RecruitmentDTO.builder()
                .saraminJobId(id)
                .companyName("회사")
                .title("백엔드 공고")
                .jobName("백엔드/서버개발")
                .jobUrl("https://example.invalid/job/" + id)
                .isActive(1)
                .build();
    }
}
