package com.provit.dao.community.impl;

import static com.provit.persistence.JpaQueries.*;
import static com.provit.persistence.JpaStore.params;

import com.provit.dao.community.CommentDAO;
import com.provit.dto.community.CommentDTO;
import com.provit.persistence.*;
import com.provit.persistence.entity.*;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Repository
@Transactional(readOnly = true)
public class CommentDAOImpl implements CommentDAO {
    private final JpaStore store;

    public CommentDAOImpl(JpaStore store) {
        this.store = store;
    }

    public List<CommentDTO> selectCommentList(Long postNum) {
        return store.nativeList(COMMENT_LIST, CommentDTO.class, params("postNum", postNum));
    }

    public List<CommentDTO> selectMyCommentList(Long userNum) {
        return store.nativeList(COMMENT_MY_LIST, CommentDTO.class, params("value", userNum));
    }

    public CommentDTO selectCommentDetail(Long id) {
        return store.one(
                CommentEntity.class, CommentDTO.class, "where e.commentNum=:id", params("id", id));
    }

    @Transactional
    public int insertComment(CommentDTO value) {
        return store.insert(
                CommentEntity.class, value, Map.of(), Set.of("commentDate", "isDeleted"));
    }

    @Transactional
    public int updateComment(CommentDTO value) {
        return store.modify(
                "update CommentEntity e set e.commentContent=:content,e.commentDate=local datetime"
                        + " where e.commentNum=:id and e.userNum=:user and e.isDeleted=0",
                params(
                        "id",
                        value.getCommentNum(),
                        "user",
                        value.getUserNum(),
                        "content",
                        value.getCommentContent()));
    }

    @Transactional
    public int deleteComment(Map<String, Object> values) {
        String owner = "ADMIN".equals(values.get("userType")) ? "" : " and e.userNum=:user";
        return store.modify(
                "update CommentEntity e set e.isDeleted=1,e.commentContent='삭제된 댓글입니다.' where"
                        + " e.commentNum=:id and e.isDeleted=0"
                        + owner,
                params("id", values.get("commentNum"), "user", values.get("userNum")));
    }
}
