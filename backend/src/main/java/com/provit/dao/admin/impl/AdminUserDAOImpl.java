package com.provit.dao.admin.impl;

import static com.provit.persistence.JpaQueries.*;
import static com.provit.persistence.JpaStore.params;

import com.provit.dao.admin.AdminUserDAO;
import com.provit.dto.admin.AdminUserDTO;
import com.provit.persistence.*;
import com.provit.persistence.entity.*;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Repository
@Transactional(readOnly = true)
public class AdminUserDAOImpl implements AdminUserDAO {
    private final JpaStore store;

    public AdminUserDAOImpl(JpaStore store) {
        this.store = store;
    }

    private String filter(Map<String, Object> values) {
        String where = "where e.userType<>'ADMIN'";
        if (values.get("keyword") instanceof String keyword && !keyword.isEmpty())
            where +=
                    " and (e.userEmail like concat('%',:keyword,'%') or e.userNickname like"
                            + " concat('%',:keyword,'%'))";
        if ("BLOCKED".equals(values.get("status"))) where += " and e.blockedDate>local datetime";
        if ("ACTIVE".equals(values.get("status")))
            where += " and (e.blockedDate is null or e.blockedDate<=local datetime)";
        return where;
    }

    public List<AdminUserDTO> selectUserList(Map<String, Object> values) {
        return store.list(
                UserEntity.class,
                AdminUserDTO.class,
                filter(values) + " order by e.userNum desc",
                values,
                ((Number) values.get("offset")).intValue(),
                ((Number) values.get("limit")).intValue(),
                Map.of());
    }

    public int selectUserCount(Map<String, Object> values) {
        return Math.toIntExact(store.count(UserEntity.class, filter(values), values));
    }

    @Transactional
    public void updateBlockedDate(Map<String, Object> values) {
        int days = ((Number) values.get("blockDays")).intValue();
        LocalDateTime until =
                days == 0
                        ? null
                        : days == 9999
                                ? LocalDate.of(2099, 12, 31).atStartOfDay()
                                : LocalDateTime.now(DtoMapper.ZONE).plusDays(days);
        store.patch(
                UserEntity.class,
                "where e.userNum=:id",
                params("id", values.get("userNum")),
                params("blockedDate", until));
    }
}
