package com.provit.dao.community.impl;

import static com.provit.persistence.JpaQueries.*;
import static com.provit.persistence.JpaStore.params;

import com.provit.dao.community.ReportDAO;
import com.provit.dto.community.ReportDTO;
import com.provit.persistence.*;
import com.provit.persistence.entity.*;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Repository
@Transactional(readOnly = true)
public class ReportDAOImpl implements ReportDAO {
    private final JpaStore store;

    public ReportDAOImpl(JpaStore store) {
        this.store = store;
    }

    public int checkDuplicateReport(ReportDTO value) {
        return Math.toIntExact(
                store.count(
                        ReportEntity.class,
                        "where e.reporterNum=:user and e.targetType=:type and e.targetNum=:id",
                        params(
                                "user",
                                value.getReporterNum().intValue(),
                                "type",
                                value.getTargetType(),
                                "id",
                                value.getTargetNum())));
    }

    @Transactional
    public void insertReport(ReportDTO value) {
        store.insert(ReportEntity.class, value, Map.of(), Set.of("reportDate", "reportStatus"));
    }
}
