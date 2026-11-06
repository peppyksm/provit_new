package com.provit.dao.study.impl;

import static com.provit.persistence.JpaQueries.*;
import static com.provit.persistence.JpaStore.params;

import com.provit.dao.study.StudyDAO;
import com.provit.dto.auth.UserDTO;
import com.provit.dto.study.StudyDTO;
import com.provit.persistence.*;
import com.provit.persistence.entity.*;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Repository
@Transactional(readOnly = true)
public class StudyDAOImpl implements StudyDAO {
    private final JpaStore store;

    public StudyDAOImpl(JpaStore store) {
        this.store = store;
    }

    private static final String DETAILS =
            "select s.STUDY_NUM as studyNum,s.USER_NUM as userNum,u.USER_NICKNAME as"
                + " userNickname,s.STUDY_NAME as studyName,s.STUDY_EXPLAIN as"
                + " studyExplain,s.MAX_MEMBERS as"
                + " maxMembers,to_char(s.STUDY_CREATE_DATE,'YYYY-MM-DD HH24:MI:SS') as"
                + " studyCreateDate,(select count(*) from T_STUDY_MEMBER m where"
                + " m.STUDY_NUM=s.STUDY_NUM) as memberCount,case when exists(select 1 from"
                + " T_STUDY_MEMBER m where m.STUDY_NUM=s.STUDY_NUM and m.USER_NUM=:userNum) then 1"
                + " else 0 end as isJoined from T_STUDY s join T_USER u on u.USER_NUM=s.USER_NUM ";

    public List<StudyDTO> selectStudyList(Long userNum) {
        return store.nativeList(
                DETAILS + "order by s.STUDY_NUM desc", StudyDTO.class, params("userNum", userNum));
    }

    public StudyDTO selectStudyDetail(Long studyNum) {
        return store.nativeOne(
                DETAILS + "where s.STUDY_NUM=:id",
                StudyDTO.class,
                params("userNum", null, "id", studyNum));
    }

    @Transactional
    public StudyDTO selectStudyDetailForUpdate(Long studyNum) {
        if (store.lock(StudyEntity.class, studyNum) == null) return null;
        return selectStudyDetail(studyNum);
    }

    @Transactional
    public int insertStudy(StudyDTO value) {
        return store.insert(StudyEntity.class, value, Map.of(), Set.of("studyCreateDate"));
    }

    @Transactional
    public int updateStudy(StudyDTO value) {
        return store.patch(
                StudyEntity.class,
                "where e.studyNum=:id and e.userNum=:user",
                params("id", value.getStudyNum(), "user", value.getUserNum()),
                params(
                        "studyName",
                        value.getStudyName(),
                        "studyExplain",
                        value.getStudyExplain(),
                        "maxMembers",
                        value.getMaxMembers()));
    }

    @Transactional
    public int deleteStudy(Map<String, Object> values) {
        return store.modify(
                "delete from StudyEntity e where e.studyNum=:id"
                        + ("ADMIN".equals(values.get("userType")) ? "" : " and e.userNum=:user"),
                params("id", values.get("studyNum"), "user", values.get("userNum")));
    }

    @Transactional
    public int insertStudyMember(Map<String, Object> values) {
        return store.insert(StudyMemberEntity.class, values, Map.of(), Set.of("studyJoinDate"));
    }

    @Transactional
    public int deleteStudyMember(Map<String, Object> values) {
        return store.modify(
                "delete from StudyMemberEntity e where e.studyNum=:study and e.userNum=:user",
                params("study", values.get("studyNum"), "user", values.get("userNum")));
    }

    public int checkStudyMember(Map<String, Object> values) {
        return Math.toIntExact(
                store.count(
                        StudyMemberEntity.class,
                        "where e.studyNum=:study and e.userNum=:user",
                        params("study", values.get("studyNum"), "user", values.get("userNum"))));
    }

    public List<String> selectStudyMembers(Long studyNum) {
        return store
                .nativeList(
                        "select u.USER_NICKNAME as userNickname from T_STUDY_MEMBER m join T_USER u"
                                + " on u.USER_NUM=m.USER_NUM where m.STUDY_NUM=:id",
                        UserDTO.class,
                        params("id", studyNum))
                .stream()
                .map(UserDTO::getUserNickname)
                .toList();
    }
}
