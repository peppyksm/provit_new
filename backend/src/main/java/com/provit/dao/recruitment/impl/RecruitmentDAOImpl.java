package com.provit.dao.recruitment.impl;

import static com.provit.persistence.JpaQueries.*;
import static com.provit.persistence.JpaStore.params;

import com.provit.dao.recruitment.RecruitmentDAO;
import com.provit.dto.recruitment.*;
import com.provit.dto.user.UserJobPreferenceDTO;
import com.provit.persistence.*;
import com.provit.persistence.entity.*;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Repository
@Transactional(readOnly = true)
public class RecruitmentDAOImpl implements RecruitmentDAO {
    private final JpaStore store;

    public RecruitmentDAOImpl(JpaStore store) {
        this.store = store;
    }

    private static final String COLUMNS =
            "select r.*,case when exists(select 1 from T_JOB_SCRAP s where"
                + " s.RECRUITMENT_NUM=r.RECRUITMENT_NUM and s.USER_NUM=:userNum) then 1 else 0 end"
                + " as isScrapped from T_RECRUITMENT r ";

    public List<OccupationDTO> selectOccupationList() {
        return store.list(
                OccupationEntity.class,
                OccupationDTO.class,
                "order by cast(e.occupationCode as integer)",
                Map.of());
    }

    public List<JobDTO> selectJobListByOccupation(String code) {
        return store.list(
                JobEntity.class,
                JobDTO.class,
                "where e.occupationCode=:code order by e.jobName",
                params("code", code));
    }

    @Transactional
    public int mergeRecruitment(RecruitmentDTO value) {
        return store.nativeModify(RECRUITMENT_UPSERT, DtoMapper.values(value));
    }

    private String filter(RecruitmentSearchDTO value) {
        String where =
                Boolean.TRUE.equals(value.getScrapOnly()) && value.getUserNum() != null
                        ? "where exists(select 1 from T_JOB_SCRAP s2 where"
                                + " s2.RECRUITMENT_NUM=r.RECRUITMENT_NUM and s2.USER_NUM=:userNum)"
                        : "where r.IS_ACTIVE=1";
        if (value.getOccupationCode() != null && !value.getOccupationCode().isEmpty())
            where +=
                    " and exists(select 1 from T_JOB j where j.OCCUPATION_CODE=:occupationCode and"
                        + " (lower(r.JOB_NAME) like '%'||lower(j.JOB_NAME)||'%' or lower(r.TITLE)"
                        + " like '%'||lower(j.JOB_NAME)||'%'))";
        if (value.getKeyword() != null && !value.getKeyword().isEmpty())
            where +=
                    " and (lower(r.TITLE) like '%'||lower(:keyword)||'%' or lower(r.COMPANY_NAME)"
                            + " like '%'||lower(:keyword)||'%' or lower(r.JOB_NAME) like"
                            + " '%'||lower(:keyword)||'%')";
        if (value.getLocation() != null && !value.getLocation().isEmpty())
            where += " and r.LOCATION_NAME like '%'||:location||'%'";
        if (value.getExperienceLevel() != null && !value.getExperienceLevel().isEmpty())
            where += " and r.EXPERIENCE_LEVEL like '%'||:experienceLevel||'%'";
        return where;
    }

    public List<RecruitmentDTO> selectRecruitmentList(RecruitmentSearchDTO value) {
        String order =
                "CLOSING_SOON".equals(value.getSortBy())
                        ? "order by case when r.EXPIRATION_DATE is not null and"
                              + " r.EXPIRATION_DATE>=CURRENT_DATE then 1 when r.EXPIRATION_DATE is"
                              + " null then 2 else 3 end,case when r.EXPIRATION_DATE is not null"
                              + " and r.EXPIRATION_DATE>=CURRENT_DATE then r.EXPIRATION_DATE else"
                              + " null end asc nulls last,r.CREATED_AT desc,r.RECRUITMENT_NUM desc"
                        : "order by r.CREATED_AT desc,r.RECRUITMENT_NUM desc";
        return store.nativeList(
                COLUMNS
                        + filter(value)
                        + " "
                        + order
                        + " offset :offset rows fetch next :size rows only",
                RecruitmentDTO.class,
                DtoMapper.values(value));
    }

    public long selectRecruitmentCount(RecruitmentSearchDTO value) {
        return store.scalar(
                "select count(*) from T_RECRUITMENT r " + filter(value),
                Long.class,
                DtoMapper.values(value));
    }

    @Transactional
    public int deactivateExpiredRecruitments() {
        return store.nativeModify(EXPIRE_RECRUITMENTS, Map.of());
    }

    @Transactional
    public int purgeOldUnscrappedRecruitments() {
        return store.nativeModify(PURGE_RECRUITMENTS, Map.of());
    }

    @Transactional
    public int insertJobScrap(long recruitmentNum, long userNum) {
        return store.nativeModify(
                SCRAP_INSERT, params("recruitmentNum", recruitmentNum, "userNum", userNum));
    }

    @Transactional
    public int deleteJobScrap(long recruitment, long user) {
        return store.modify(
                "delete from JobScrapEntity e where e.recruitmentNum=:id and e.userNum=:user",
                params("id", recruitment, "user", user));
    }

    public int checkJobScrap(long recruitment, long user) {
        return Math.toIntExact(
                store.count(
                        JobScrapEntity.class,
                        "where e.recruitmentNum=:id and e.userNum=:user",
                        params("id", recruitment, "user", user)));
    }

    public UserJobPreferenceDTO selectUserJobPreference(long userNum) {
        return store.nativeOne(
                USER_PREFERENCE, UserJobPreferenceDTO.class, params("userNum", userNum));
    }

    public List<RecruitmentDTO> selectRecruitmentsByKeyword(
            String keyword, Long userNum, int limit) {
        return store.nativeList(
                COLUMNS
                        + "where r.IS_ACTIVE=1 and (lower(r.JOB_NAME) like"
                        + " '%'||lower(:keyword)||'%' or lower(r.TITLE) like"
                        + " '%'||lower(:keyword)||'%') order by r.RECRUITMENT_NUM desc fetch first"
                        + " :limit rows only",
                RecruitmentDTO.class,
                params("keyword", keyword, "userNum", userNum, "limit", limit));
    }

    public List<RecruitmentDTO> selectHotRecruitments(Long userNum, int limit) {
        return store.nativeList(
                COLUMNS
                        + "where r.IS_ACTIVE=1 order by r.RECRUITMENT_NUM desc fetch first :limit"
                        + " rows only",
                RecruitmentDTO.class,
                params("userNum", userNum, "limit", limit));
    }
}
