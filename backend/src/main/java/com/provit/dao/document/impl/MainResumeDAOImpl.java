package com.provit.dao.document.impl;

import static com.provit.persistence.JpaQueries.*;
import static com.provit.persistence.JpaStore.params;

import com.provit.dao.document.MainResumeDAO;
import com.provit.dto.document.MainResumeJobInfoDTO;
import com.provit.persistence.*;
import com.provit.persistence.entity.*;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Repository
@Transactional(readOnly = true)
public class MainResumeDAOImpl implements MainResumeDAO {
    private final JpaStore store;

    public MainResumeDAOImpl(JpaStore store) {
        this.store = store;
    }

    @Transactional
    public int updateMainResume(long user, long resume) {
        return store.modify(
                "update UserEntity u set u.mainResumeNum=:resume where u.userNum=:user and"
                    + " u.userIsDeleted=0 and exists (select r.resumeNum from ResumeEntity r where"
                    + " r.resumeNum=:resume and r.userNum=u.userNum)",
                params("resume", resume, "user", Math.toIntExact(user)));
    }

    @Transactional
    public int clearMainResume(long user) {
        return store.modify(
                "update UserEntity u set u.mainResumeNum=null where u.userNum=:user and"
                        + " u.userIsDeleted=0",
                params("user", Math.toIntExact(user)));
    }

    public MainResumeJobInfoDTO selectMainResumeJobInfo(long userNum) {
        return store.nativeOne(MAIN_RESUME, MainResumeJobInfoDTO.class, params("userNum", userNum));
    }
}
