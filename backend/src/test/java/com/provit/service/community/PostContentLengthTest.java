package com.provit.service.community;

import static org.junit.Assert.*;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import com.provit.dao.community.PostDAO;
import com.provit.dto.community.PostDTO;
import com.provit.service.community.impl.PostServiceImpl;

public class PostContentLengthTest {
    private PostServiceImpl service;
    private AtomicInteger writes;

    @Before
    public void setUp() {
        writes = new AtomicInteger();
        PostDAO dao = (PostDAO) Proxy.newProxyInstance(PostDAO.class.getClassLoader(),
                new Class<?>[] { PostDAO.class }, (proxy, method, args) -> switch (method.getName()) {
                    case "insertPost", "updatePost" -> { writes.incrementAndGet(); yield 1; }
                    default -> throw new AssertionError("Unexpected DAO call: " + method.getName());
                });
        service = new PostServiceImpl(dao, null);
    }

    @Test
    public void asciiKoreanEmojiAndMixedContentFitAtTheStorageBoundary() {
        for (String content : List.of("A".repeat(4000), "가".repeat(1333), "😀".repeat(1000),
                "가".repeat(1332) + "ABCD", "**" + "가".repeat(1332) + "**")) {
            var post = post(content);
            service.createPost(post);
            service.updatePost(post);
            assertEquals(content, post.getPostContent());
        }
        assertEquals(10, writes.get());
    }

    @Test
    public void oversizedContentIsRejectedBeforeCreatingAnyPost() {
        for (String content : oversizedContent()) {
            assertThrows(IllegalArgumentException.class, () -> service.createPost(post(content)));
        }
        assertEquals(0, writes.get());
    }

    @Test
    public void oversizedContentCannotOverwriteAnExistingPost() {
        for (String content : oversizedContent()) {
            assertThrows(IllegalArgumentException.class, () -> service.updatePost(post(content)));
        }
        assertEquals(0, writes.get());
    }

    private List<String> oversizedContent() {
        return List.of("A".repeat(4001), "가".repeat(1334), "가".repeat(1400), "😀".repeat(1001),
                "가".repeat(1332) + "ABCDE", "**" + "가".repeat(1332) + "**\n");
    }

    private PostDTO post(String content) {
        var post = new PostDTO();
        post.setPostNum(10L);
        post.setUserNum(7L);
        post.setPostContent(content);
        return post;
    }
}
