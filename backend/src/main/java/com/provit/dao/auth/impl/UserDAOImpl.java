package com.provit.dao.auth.impl;

import static com.provit.persistence.JpaQueries.*;
import static com.provit.persistence.JpaStore.params;

import com.provit.dao.auth.UserDAO;
import com.provit.dto.auth.UserDTO;
import com.provit.persistence.*;
import com.provit.persistence.entity.*;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Repository
@Transactional(readOnly = true)
public class UserDAOImpl implements UserDAO {
    private final JpaStore store;

    public UserDAOImpl(JpaStore store) {
        this.store = store;
    }

    public UserDTO selectByEmail(String email) {
        return store.one(
                UserEntity.class,
                UserDTO.class,
                "where e.userEmail=:email",
                params("email", email));
    }

    public UserDTO selectByNickname(String nickname) {
        return store.one(
                UserEntity.class,
                UserDTO.class,
                "where e.userNickname=:nickname and e.userIsDeleted=0",
                params("nickname", nickname));
    }

    public UserDTO selectByUserNum(Long id) {
        return store.one(
                UserEntity.class,
                UserDTO.class,
                "where e.userNum=:id",
                params("id", Math.toIntExact(id)));
    }

    public int countByEmail(String email) {
        return Math.toIntExact(
                store.count(UserEntity.class, "where e.userEmail=:email", params("email", email)));
    }

    public int countByNickname(String nickname) {
        return Math.toIntExact(
                store.count(
                        UserEntity.class,
                        "where e.userNickname=:nickname and e.userIsDeleted=0",
                        params("nickname", nickname)));
    }

    @Transactional
    public int insertUser(UserDTO value) {
        return store.insert(
                UserEntity.class,
                value,
                Map.of(),
                Set.of(
                        "userRegisterDate",
                        "userIsDeleted",
                        "userTokenVersion",
                        "blockedDate",
                        "mainResumeNum"));
    }

    @Transactional
    public int updateMyProfile(UserDTO value) {
        Map<String, Object> changes = new LinkedHashMap<>();
        if (value.getUserNickname() != null) changes.put("userNickname", value.getUserNickname());
        if (value.getUserPw() != null) changes.put("userPw", value.getUserPw());
        if (changes.isEmpty()) return 0;
        String assignments =
                changes.keySet().stream()
                        .map(name -> "e." + name + "=:" + name)
                        .collect(java.util.stream.Collectors.joining(","));
        changes.put("id", Math.toIntExact(value.getUserNum()));
        return store.modify(
                "update UserEntity e set "
                        + assignments
                        + " where e.userNum=:id and e.userIsDeleted=0",
                changes);
    }

    @Transactional
    public int updatePasswordByEmail(UserDTO value) {
        return store.patch(
                UserEntity.class,
                "where e.userEmail=:email and e.userIsDeleted=0",
                params("email", value.getUserEmail()),
                params("userPw", value.getUserPw()));
    }

    @Transactional
    public int incrementTokenVersion(Long id) {
        return store.modify(
                "update UserEntity e set e.userTokenVersion=e.userTokenVersion+1 where"
                        + " e.userNum=:id and e.userIsDeleted=0",
                params("id", Math.toIntExact(id)));
    }

    @Transactional
    public int withdrawMyAccount(Long id) {
        return store.modify(
                "update UserEntity e set e.userIsDeleted=1,e.userTokenVersion=e.userTokenVersion+1"
                        + " where e.userNum=:id and e.userIsDeleted=0",
                params("id", Math.toIntExact(id)));
    }
}
