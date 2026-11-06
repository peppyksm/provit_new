package com.provit.dao.community.impl;

import static com.provit.persistence.JpaQueries.*;
import static com.provit.persistence.JpaStore.params;

import com.provit.dao.community.PostDAO;
import com.provit.dto.community.*;
import com.provit.persistence.*;
import com.provit.persistence.entity.*;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Repository
@Transactional(readOnly = true)
public class PostDAOImpl implements PostDAO {
    private final JpaStore store;

    public PostDAOImpl(JpaStore store) {
        this.store = store;
    }

    private static final String JOINS =
            " from T_POST p join T_CATEGORY c on c.CATEGORY_NUM=p.CATEGORY_NUM join T_USER u on"
                    + " u.USER_NUM=p.USER_NUM ";
    private static final String COLUMNS =
            "select"
                + " p.POST_NUM,p.CATEGORY_NUM,c.CATEGORY_NAME,p.USER_NUM,u.USER_NICKNAME,p.POST_TITLE,p.POST_LIKE_COUNT,p.VIEW_COUNT,p.POST_DATE,(select"
                + " count(*) from T_COMMENT c2 where c2.POST_NUM=p.POST_NUM) as commentCount";

    private String filter(PostSearchDTO value) {
        String where = " where 1=1";
        if (value.getAuthorUserNum() != null) where += " and p.USER_NUM=:author";
        if (value.getCategoryNum() != null) where += " and p.CATEGORY_NUM=:category";
        if (value.getKeyword() != null && !value.getKeyword().isEmpty()) {
            where += " and p.POST_TITLE<>'관리자에 의해 블라인드 처리된 게시물입니다.' and ";
            where +=
                    switch (value.getSearchType() == null ? "" : value.getSearchType()) {
                        case "TITLE" -> "p.POST_TITLE like '%'||:keyword||'%'";
                        case "CONTENT" -> "p.POST_CONTENT like '%'||:keyword||'%'";
                        case "WRITER" -> "u.USER_NICKNAME like '%'||:keyword||'%'";
                        default ->
                                "(p.POST_TITLE like '%'||:keyword||'%' or p.POST_CONTENT like"
                                        + " '%'||:keyword||'%' or u.USER_NICKNAME like"
                                        + " '%'||:keyword||'%')";
                    };
        }
        return where;
    }

    private Map<String, Object> values(PostSearchDTO value) {
        return params(
                "author",
                value.getAuthorUserNum(),
                "category",
                value.getCategoryNum(),
                "keyword",
                value.getKeyword(),
                "offset",
                value.getOffset(),
                "limit",
                value.getPageSize());
    }

    public List<PostDTO> selectPostList(PostSearchDTO value) {
        return store.nativeList(
                COLUMNS
                        + JOINS
                        + filter(value)
                        + " order by p.POST_NUM desc offset :offset rows fetch next :limit rows"
                        + " only",
                PostDTO.class,
                values(value));
    }

    public int countPosts(PostSearchDTO value) {
        return Math.toIntExact(
                store.scalar("select count(*)" + JOINS + filter(value), Long.class, values(value)));
    }

    public PostDTO selectPostDetail(Long postNum) {
        return store.nativeOne(POST_DETAIL, PostDTO.class, params("postNum", postNum));
    }

    @Transactional
    public int updateViewCount(Long id) {
        return store.modify(
                "update PostEntity e set e.viewCount=e.viewCount+1 where e.postNum=:id",
                params("id", id));
    }

    @Transactional
    public int insertPost(PostDTO value) {
        return store.insert(
                PostEntity.class,
                value,
                Map.of(),
                Set.of("postDate", "postLikeCount", "viewCount"));
    }

    @Transactional
    public int updatePost(PostDTO value) {
        return store.modify(
                "update PostEntity e set"
                    + " e.categoryNum=:category,e.postTitle=:title,e.postContent=:content,e.postFile=:file"
                    + " where e.postNum=:id and e.userNum=:user and e.postTitle<>'관리자에 의해 블라인드 처리된"
                    + " 게시물입니다.' and not exists(select r.reportNum from ReportEntity r where"
                    + " r.targetType='POST' and r.targetNum=:id and r.reportStatus='RESOLVED')",
                params(
                        "id",
                        value.getPostNum(),
                        "user",
                        value.getUserNum(),
                        "category",
                        value.getCategoryNum(),
                        "title",
                        value.getPostTitle(),
                        "content",
                        value.getPostContent(),
                        "file",
                        value.getPostFile()));
    }

    @Transactional
    public int deletePost(Map<String, Object> values) {
        return store.modify(
                "delete from PostEntity e where e.postNum=:id"
                        + ("ADMIN".equals(values.get("userType")) ? "" : " and e.userNum=:user"),
                params("id", values.get("postNum"), "user", values.get("userNum")));
    }

    public int checkPostLike(Map<String, Object> values) {
        return Math.toIntExact(
                store.count(
                        PostLikeEntity.class,
                        "where e.postNum=:post and e.userNum=:user",
                        params("post", values.get("postNum"), "user", values.get("userNum"))));
    }

    @Transactional
    public int insertPostLike(Map<String, Object> values) {
        return store.insert(PostLikeEntity.class, values, Map.of(), Set.of("createdAt"));
    }

    @Transactional
    public int deletePostLike(Map<String, Object> values) {
        return store.modify(
                "delete from PostLikeEntity e where e.postNum=:post and e.userNum=:user",
                params("post", values.get("postNum"), "user", values.get("userNum")));
    }

    @Transactional
    public int updatePostLikeCount(Map<String, Object> values) {
        return store.modify(
                "update PostEntity e set e.postLikeCount=e.postLikeCount+:amount where"
                        + " e.postNum=:id",
                params("amount", values.get("amount"), "id", values.get("postNum")));
    }

    public List<PostDTO> selectPopularPosts(int limit) {
        return store.nativeList(
                COLUMNS
                        + JOINS
                        + "order by (p.VIEW_COUNT+p.POST_LIKE_COUNT*10) desc,p.POST_NUM desc limit"
                        + " :limit",
                PostDTO.class,
                params("limit", limit));
    }
}
