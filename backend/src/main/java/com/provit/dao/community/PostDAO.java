package com.provit.dao.community;

import com.provit.dto.community.PostDTO;
import com.provit.dto.community.PostSearchDTO;

import java.util.List;
import java.util.Map;

public interface PostDAO {
    List<PostDTO> selectPostList(PostSearchDTO searchDto);

    int countPosts(PostSearchDTO searchDto);

    PostDTO selectPostDetail(Long postNum);

    int updateViewCount(Long postNum);

    int insertPost(PostDTO postDto);

    int updatePost(PostDTO postDto);

    int deletePost(Map<String, Object> params);

    int checkPostLike(Map<String, Object> params);

    int insertPostLike(Map<String, Object> params);

    int deletePostLike(Map<String, Object> params);

    int updatePostLikeCount(Map<String, Object> params);

    List<PostDTO> selectPopularPosts(int limit);
}
