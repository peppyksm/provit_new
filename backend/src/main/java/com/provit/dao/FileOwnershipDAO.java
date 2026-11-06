package com.provit.dao;

import com.provit.dto.community.PostDTO;
import com.provit.dto.document.PortfolioDTO;
import com.provit.persistence.JpaStore;
import com.provit.persistence.entity.*;

import org.springframework.stereotype.Repository;

@Repository
public class FileOwnershipDAO {
    private final JpaStore store;

    public FileOwnershipDAO(JpaStore store) {
        this.store = store;
    }

    public int activeUserCount(long user) {
        return Math.toIntExact(
                store.count(
                        UserEntity.class,
                        "where e.userNum=:id and e.userIsDeleted=0",
                        JpaStore.params("id", user)));
    }

    public Integer portfolioOwner(long id) {
        var value =
                store.one(
                        PortfolioEntity.class,
                        PortfolioDTO.class,
                        "where e.portfolioNum=:id",
                        JpaStore.params("id", id));
        return value == null ? null : value.getUserNum();
    }

    public Integer postOwner(long id) {
        var value =
                store.one(
                        PostEntity.class,
                        PostDTO.class,
                        "where e.postNum=:id",
                        JpaStore.params("id", id));
        return value == null ? null : Math.toIntExact(value.getUserNum());
    }
}
