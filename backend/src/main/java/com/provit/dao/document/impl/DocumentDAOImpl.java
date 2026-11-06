package com.provit.dao.document.impl;

import static com.provit.persistence.JpaQueries.*;
import static com.provit.persistence.JpaStore.params;

import com.provit.dao.document.DocumentDAO;
import com.provit.dto.document.*;
import com.provit.persistence.*;
import com.provit.persistence.entity.*;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Repository
@Transactional(readOnly = true)
public class DocumentDAOImpl implements DocumentDAO {
    private final JpaStore store;

    public DocumentDAOImpl(JpaStore store) {
        this.store = store;
    }

    public ResumeDTO selectResumeJob(String occupationCode, String jobCode) {
        return store.nativeOne(
                RESUME_JOB,
                ResumeDTO.class,
                params("occupationCode", occupationCode, "jobCode", jobCode));
    }

    @Transactional
    public int selectNextPortfolioNum() {
        return store.scalar("select nextval('seq_t_portfolio')", Integer.class, Map.of());
    }

    @Transactional
    public int insertResume(ResumeDTO value) {
        return store.insert(ResumeEntity.class, value, Map.of(), Set.of("createdAt", "updatedAt"));
    }

    @Transactional
    public int insertEducation(EducationDTO value) {
        return store.insert(EducationEntity.class, value, Map.of());
    }

    @Transactional
    public int insertCareer(CareerDTO value) {
        return store.insert(CareerEntity.class, value, Map.of());
    }

    @Transactional
    public int insertCertification(CertificationDTO value) {
        return store.insert(CertificationEntity.class, value, Map.of());
    }

    @Transactional
    public int insertPortfolio(PortfolioDTO value) {
        return store.insert(
                PortfolioEntity.class, value, Map.of(), Set.of("createdAt", "updatedAt"));
    }

    @Transactional
    public int insertCoverLetter(CoverLetterDTO value) {
        return store.insert(
                CoverLetterEntity.class, value, Map.of(), Set.of("createdAt", "updatedAt"));
    }

    @Transactional
    public int updateResume(ResumeDTO value) {
        return store.patch(
                ResumeEntity.class,
                "where e.resumeNum=:id and e.userNum=:user",
                params("id", (long) value.getResumeNum(), "user", value.getUserNum()),
                params(
                        "resumeTitle",
                        value.getResumeTitle(),
                        "occupationCode",
                        value.getOccupationCode(),
                        "jobCode",
                        value.getJobCode(),
                        "highestLevel",
                        value.getHighestLevel(),
                        "motivation",
                        value.getMotivation(),
                        "desiredLocation",
                        value.getDesiredLocation(),
                        "desiredWorkType",
                        value.getDesiredWorkType()));
    }

    @Transactional
    public int updateCoverLetter(CoverLetterDTO value) {
        return store.patch(
                CoverLetterEntity.class,
                "where e.letterNum=:id and e.userNum=:user",
                params("id", (long) value.getLetterNum(), "user", value.getUserNum()),
                params(
                        "coverLetterTitle",
                        value.getCoverLetterTitle(),
                        "growthProcess",
                        value.getGrowthProcess(),
                        "personalityStrengthsWeaknesses",
                        value.getPersonalityStrengthsWeaknesses(),
                        "problemSolvingExperience",
                        value.getProblemSolvingExperience(),
                        "postJoiningAspiration",
                        value.getPostJoiningAspiration()));
    }

    @Transactional
    public int deleteEducationList(int resumeNum) {
        return store.modify(
                "delete from EducationEntity e where e.resumeNum=:id",
                params("id", (long) resumeNum));
    }

    @Transactional
    public int deleteCareerList(int resumeNum) {
        return store.modify(
                "delete from CareerEntity e where e.resumeNum=:id", params("id", (long) resumeNum));
    }

    @Transactional
    public int deleteCertificationList(int resumeNum) {
        return store.modify(
                "delete from CertificationEntity e where e.resumeNum=:id",
                params("id", (long) resumeNum));
    }

    @Transactional
    public int deleteResume(int userNum, int id) {
        return store.modify(
                "delete from ResumeEntity e where e.resumeNum=:id and e.userNum=:user",
                params("id", (long) id, "user", userNum));
    }

    @Transactional
    public int deleteCoverLetter(int userNum, int id) {
        return store.modify(
                "delete from CoverLetterEntity e where e.letterNum=:id and e.userNum=:user",
                params("id", (long) id, "user", userNum));
    }

    @Transactional
    public int deletePortfolio(int userNum, int id) {
        return store.modify(
                "delete from PortfolioEntity e where e.portfolioNum=:id and e.userNum=:user",
                params("id", (long) id, "user", userNum));
    }

    public ResumeDTO selectResume(int userNum, int resumeNum) {
        return store.nativeOne(
                RESUME_DETAIL, ResumeDTO.class, params("userNum", userNum, "resumeNum", resumeNum));
    }

    public CoverLetterDTO selectCoverLetter(int userNum, int id) {
        return store.one(
                CoverLetterEntity.class,
                CoverLetterDTO.class,
                "where e.letterNum=:id and e.userNum=:user",
                params("id", (long) id, "user", userNum));
    }

    public PortfolioDTO selectPortfolio(int userNum, int id) {
        return store.one(
                PortfolioEntity.class,
                PortfolioDTO.class,
                "where e.portfolioNum=:id and e.userNum=:user",
                params("id", (long) id, "user", userNum));
    }

    public PortfolioDTO selectPortfolioByPortfolioNum(int id) {
        return store.one(
                PortfolioEntity.class,
                PortfolioDTO.class,
                "where e.portfolioNum=:id",
                params("id", (long) id));
    }

    public List<EducationDTO> selectEducationList(int id) {
        return store.list(
                EducationEntity.class,
                EducationDTO.class,
                "where e.resumeNum=:id order by e.admissionDate desc,e.eduNum desc",
                params("id", (long) id));
    }

    public List<CareerDTO> selectCareerList(int id) {
        return store.list(
                CareerEntity.class,
                CareerDTO.class,
                "where e.resumeNum=:id order by e.joinDate desc,e.careerNum desc",
                params("id", (long) id));
    }

    public List<CertificationDTO> selectCertificationList(int id) {
        return store.list(
                CertificationEntity.class,
                CertificationDTO.class,
                "where e.resumeNum=:id order by e.issueDate desc,e.certNum desc",
                params("id", (long) id));
    }

    public List<DocumentSummaryDTO> selectResumeSummaryList(int user) {
        return store.project(
                "select e.resumeNum as documentNum,e.resumeTitle as documentTitle,e.createdAt as"
                    + " createdAt from ResumeEntity e where e.userNum=:user order by e.createdAt"
                    + " desc,e.resumeNum desc",
                DocumentSummaryDTO.class,
                params("user", user));
    }

    public List<DocumentSummaryDTO> selectCoverLetterSummaryList(int user) {
        return store.project(
                "select e.letterNum as documentNum,e.coverLetterTitle as documentTitle,e.createdAt"
                        + " as createdAt from CoverLetterEntity e where e.userNum=:user order by"
                        + " e.createdAt desc,e.letterNum desc",
                DocumentSummaryDTO.class,
                params("user", user));
    }

    public List<DocumentSummaryDTO> selectPortfolioSummaryList(int user) {
        return store.project(
                "select e.portfolioNum as documentNum,e.portfolioTitle as documentTitle,e.createdAt"
                        + " as createdAt from PortfolioEntity e where e.userNum=:user order by"
                        + " e.createdAt desc,e.portfolioNum desc",
                DocumentSummaryDTO.class,
                params("user", user));
    }
}
