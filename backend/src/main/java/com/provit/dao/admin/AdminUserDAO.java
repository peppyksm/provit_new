package com.provit.dao.admin;

import com.provit.dto.admin.AdminUserDTO;

import java.util.List;
import java.util.Map;

public interface AdminUserDAO {
    List<AdminUserDTO> selectUserList(Map<String, Object> params);

    int selectUserCount(Map<String, Object> params);

    void updateBlockedDate(Map<String, Object> params);
}
