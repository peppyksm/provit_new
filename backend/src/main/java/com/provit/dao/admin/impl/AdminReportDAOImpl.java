package com.provit.dao.admin.impl;

import static com.provit.persistence.JpaQueries.*;
import static com.provit.persistence.JpaStore.params;

import com.provit.dao.admin.AdminReportDAO;
import com.provit.dto.admin.AdminReportDTO;
import com.provit.dto.auth.UserDTO;
import com.provit.persistence.*;
import com.provit.persistence.entity.*;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Repository
@Transactional(readOnly = true)
public class AdminReportDAOImpl implements AdminReportDAO {
    private final JpaStore store;

    public AdminReportDAOImpl(JpaStore store) {
        this.store = store;
    }

    private String filter(Map<String, Object> values) {
        return values.get("status") instanceof String status && !status.isEmpty()
                ? " where r.REPORT_STATUS=:status"
                : "";
    }

    public List<AdminReportDTO> selectReportList(Map<String, Object> values) {
        String sql =
                "select r.REPORT_NUM as reportNum,r.REPORTER_NUM as reporterNum,u.USER_NICKNAME as"
                    + " reporterNickname,r.TARGET_TYPE as targetType,r.TARGET_NUM as"
                    + " targetNum,r.REPORT_REASON as reportReason,r.REPORT_STATUS as"
                    + " reportStatus,r.REPORT_DATE as reportDate,coalesce(case when"
                    + " r.TARGET_TYPE='POST' then substr(p.POST_TITLE,1,50) when"
                    + " r.TARGET_TYPE='COMMENT' then substr(c.COMMENT_CONTENT,1,50) end,'내용 없음"
                    + " (삭제됨)') as targetContentPreview,c.POST_NUM as commentPostNum from T_REPORT"
                    + " r left join T_USER u on u.USER_NUM=r.REPORTER_NUM left join T_POST p on"
                    + " r.TARGET_TYPE='POST' and r.TARGET_NUM=p.POST_NUM left join T_COMMENT c on"
                    + " r.TARGET_TYPE='COMMENT' and r.TARGET_NUM=c.COMMENT_NUM";
        return store.nativeList(
                sql
                        + filter(values)
                        + " order by r.REPORT_DATE desc offset :offset rows fetch next :limit rows"
                        + " only",
                AdminReportDTO.class,
                values);
    }

    public boolean isAdmin(Long user) {
        return store.count(
                        UserEntity.class,
                        "where e.userNum=:user and e.userType='ADMIN'",
                        params("user", user))
                > 0;
    }

    public int selectReportCount(Map<String, Object> values) {
        return Math.toIntExact(
                store.scalar(
                        "select count(*) from T_REPORT r" + filter(values), Long.class, values));
    }

    public AdminReportDTO selectReportDetail(Long id) {
        return store.one(
                ReportEntity.class,
                AdminReportDTO.class,
                "where e.reportNum=:id",
                params("id", id));
    }

    @Transactional
    public int updateReportStatus(Map<String, Object> values) {
        return store.patch(
                ReportEntity.class,
                "where e.reportNum=:id",
                params("id", values.get("reportNum")),
                params("reportStatus", values.get("status")));
    }

    @Transactional
    public int blindPost(Long id) {
        return store.patch(
                PostEntity.class,
                "where e.postNum=:id",
                params("id", id),
                params(
                        "postTitle",
                        "관리자에 의해 블라인드 처리된 게시물입니다.",
                        "postContent",
                        "신고 누적으로 인하여 관리자에 의해 삭제(블라인드) 조치되었습니다.",
                        "postFile",
                        null));
    }

    @Transactional
    public int blindComment(Long id) {
        return store.patch(
                CommentEntity.class,
                "where e.commentNum=:id",
                params("id", id),
                params("commentContent", "관리자에 의해 블라인드 처리된 댓글입니다.", "isDeleted", 1));
    }

    public String getTargetUserEmail(String type, Long id) {
        if (!Set.of("POST", "COMMENT").contains(type)) return null;
        var result =
                store.nativeOne(
                        "select u.USER_EMAIL as userEmail from "
                                + ("POST".equals(type) ? "T_POST" : "T_COMMENT")
                                + " t join T_USER u on u.USER_NUM=t.USER_NUM where t."
                                + ("POST".equals(type) ? "POST_NUM" : "COMMENT_NUM")
                                + "=:id",
                        UserDTO.class,
                        params("id", id));
        return result == null ? null : result.getUserEmail();
    }
}
