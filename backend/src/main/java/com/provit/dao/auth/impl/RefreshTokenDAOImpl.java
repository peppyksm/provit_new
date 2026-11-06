package com.provit.dao.auth.impl;

import static com.provit.persistence.JpaQueries.*;
import static com.provit.persistence.JpaStore.params;

import com.provit.dao.auth.RefreshTokenDAO;
import com.provit.dto.auth.RefreshTokenDTO;
import com.provit.persistence.*;
import com.provit.persistence.entity.*;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Repository
@Transactional(readOnly = true)
public class RefreshTokenDAOImpl implements RefreshTokenDAO {
    private final JpaStore store;

    public RefreshTokenDAOImpl(JpaStore store) {
        this.store = store;
    }

    @Transactional
    public int insert(RefreshTokenDTO value) {
        return store.insert(RefreshTokenEntity.class, value, Map.of());
    }

    public RefreshTokenDTO selectByTokenId(String id) {
        return store.one(
                RefreshTokenEntity.class,
                RefreshTokenDTO.class,
                "where e.tokenId=:id",
                params("id", id));
    }

    @Transactional
    public int deleteByTokenId(String id) {
        return store.modify(
                "delete from RefreshTokenEntity e where e.tokenId=:id", params("id", id));
    }

    @Transactional
    public int deleteExpired() {
        return store.modify(
                "delete from RefreshTokenEntity e where e.expiresAt<=local datetime", Map.of());
    }
}
