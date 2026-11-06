package com.provit.dao.interview.impl;

import static com.provit.persistence.JpaQueries.*;
import static com.provit.persistence.JpaStore.params;

import com.provit.dao.interview.InterviewDAO;
import com.provit.dto.document.*;
import com.provit.dto.interview.*;
import com.provit.dto.user.UserJobPreferenceDTO;
import com.provit.persistence.*;
import com.provit.persistence.entity.*;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Repository
@Transactional(readOnly = true)
public class InterviewDAOImpl implements InterviewDAO {
    private final JpaStore store;

    public InterviewDAOImpl(JpaStore store) {
        this.store = store;
    }

    @Transactional
    public int selectNextHistoryNum() {
        return store.scalar("select nextval('seq_t_interview_history')", Integer.class, Map.of());
    }

    @Transactional
    public int insertInterviewHistory(InterviewHistoryDTO value) {
        return store.insert(InterviewHistoryEntity.class, value, Map.of());
    }

    @Transactional
    public int insertInterviewResult(InterviewResultDTO value) {
        return store.insert(
                InterviewResultEntity.class,
                value,
                Map.of(
                        "strengths",
                        "strength",
                        "weaknesses",
                        "weakness",
                        "comparison",
                        "previousComparison",
                        "improvements",
                        "improvementPoint"));
    }

    public InterviewHistoryDTO selectInterviewHistory(int history, int user) {
        return store.one(
                InterviewHistoryEntity.class,
                InterviewHistoryDTO.class,
                "where e.historyNum=:id and e.userNum=:user",
                params("id", (long) history, "user", user));
    }

    private List<InterviewResultDTO> results(String where, Map<String, Object> values, int limit) {
        return store.list(
                InterviewResultEntity.class,
                InterviewResultDTO.class,
                where,
                values,
                0,
                limit,
                Map.of(
                        "strength",
                        "strengths",
                        "weakness",
                        "weaknesses",
                        "previousComparison",
                        "comparison",
                        "improvementPoint",
                        "improvements"));
    }

    public InterviewResultDTO selectInterviewResult(int history, int user) {
        var rows =
                results(
                        "where e.historyNum=:id and e.userNum=:user",
                        params("id", (long) history, "user", user),
                        1);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<InterviewResultDTO> selectInterviewResultListByUserNum(int user) {
        return results(
                "where e.userNum=:user order by e.interviewDate desc,e.historyNum desc",
                params("user", user),
                Integer.MAX_VALUE);
    }

    public InterviewResultDTO selectLatestInterviewResultByUserNum(int user) {
        var rows =
                results(
                        "where e.userNum=:user order by e.interviewDate desc,e.historyNum desc",
                        params("user", user),
                        1);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public UserJobPreferenceDTO selectUserJobPreferenceByUserNum(int userNum) {
        return store.nativeOne(
                INTERVIEW_PREFERENCE, UserJobPreferenceDTO.class, params("userNum", userNum));
    }

    public List<InterviewDocumentOptionDTO> selectResumeListByUserNum(int user) {
        return store.project(
                "select e.resumeNum as documentNum,e.resumeTitle as documentTitle from ResumeEntity"
                        + " e where e.userNum=:user order by e.updatedAt desc,e.resumeNum desc",
                InterviewDocumentOptionDTO.class,
                params("user", user));
    }

    public List<InterviewDocumentOptionDTO> selectCoverLetterListByUserNum(int user) {
        return store.project(
                "select e.letterNum as documentNum,e.coverLetterTitle as documentTitle from"
                        + " CoverLetterEntity e where e.userNum=:user order by e.updatedAt"
                        + " desc,e.letterNum desc",
                InterviewDocumentOptionDTO.class,
                params("user", user));
    }

    public List<InterviewDocumentOptionDTO> selectPortfolioListByUserNum(int user) {
        return store.project(
                "select e.portfolioNum as documentNum,e.portfolioTitle as documentTitle from"
                        + " PortfolioEntity e where e.userNum=:user order by e.updatedAt"
                        + " desc,e.portfolioNum desc",
                InterviewDocumentOptionDTO.class,
                params("user", user));
    }

    public ResumeDTO selectResumeByResumeNumAndUserNum(int resumeNum, int userNum) {
        return store.nativeOne(
                RESUME_DETAIL, ResumeDTO.class, params("resumeNum", resumeNum, "userNum", userNum));
    }

    public CoverLetterDTO selectCoverLetterByLetterNumAndUserNum(int id, int user) {
        return store.one(
                CoverLetterEntity.class,
                CoverLetterDTO.class,
                "where e.letterNum=:id and e.userNum=:user",
                params("id", (long) id, "user", user));
    }

    public PortfolioDTO selectPortfolioByPortfolioNumAndUserNum(int id, int user) {
        return store.one(
                PortfolioEntity.class,
                PortfolioDTO.class,
                "where e.portfolioNum=:id and e.userNum=:user",
                params("id", (long) id, "user", user));
    }

    public List<EducationDTO> selectEducationListByResumeNum(int id) {
        return store.list(
                EducationEntity.class,
                EducationDTO.class,
                "where e.resumeNum=:id order by e.admissionDate desc,e.eduNum desc",
                params("id", (long) id));
    }

    public List<CareerDTO> selectCareerListByResumeNum(int id) {
        return store.list(
                CareerEntity.class,
                CareerDTO.class,
                "where e.resumeNum=:id order by e.joinDate desc,e.careerNum desc",
                params("id", (long) id));
    }

    public List<CertificationDTO> selectCertificationListByResumeNum(int id) {
        return store.list(
                CertificationEntity.class,
                CertificationDTO.class,
                "where e.resumeNum=:id order by e.issueDate desc,e.certNum desc",
                params("id", (long) id));
    }
}
