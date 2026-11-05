package com.provit.service.community;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.lang.reflect.Proxy;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.provit.common.GlobalExceptionHandler;
import com.provit.common.annotation.LoginUser;
import com.provit.controller.community.PostController;
import com.provit.dao.community.PostDAO;
import com.provit.dto.community.PostDTO;
import com.provit.service.community.impl.PostServiceImpl;

public class PostBlindProtectionTest {
    private PostServiceImpl service;
    private PostDTO storedPost;
    private int affectedRows;
    private int detailReads;

    @Before
    public void setUp() {
        affectedRows = 1;
        storedPost = new PostDTO();
        storedPost.setPostNum(10L);
        storedPost.setIsBlinded(false);
        PostDAO dao = (PostDAO) Proxy.newProxyInstance(PostDAO.class.getClassLoader(),
                new Class<?>[] { PostDAO.class }, (proxy, method, args) -> switch (method.getName()) {
                    case "updatePost" -> affectedRows;
                    case "selectPostDetail" -> {
                        detailReads++;
                        yield storedPost;
                    }
                    default -> throw new AssertionError("Unexpected DAO call: " + method.getName());
                });
        service = new PostServiceImpl(dao, null);
    }

    @Test
    public void ordinaryPostCanStillBeEdited() {
        service.updatePost(edit());
        assertEquals(0, detailReads);
    }

    @Test
    public void rejectedBlindEditUsesServerStateInsteadOfRequestFlag() {
        affectedRows = 0;
        storedPost.setIsBlinded(true);
        var exception = assertThrows(SecurityException.class, () -> service.updatePost(edit()));
        assertTrue(exception.getMessage().contains("블라인드"));
        assertEquals(1, detailReads);
    }

    @Test
    public void missingPostRetainsTheExistingError() {
        affectedRows = 0;
        storedPost = null;
        assertThrows(IllegalArgumentException.class, () -> service.updatePost(edit()));
    }

    @Test
    public void foreignPostRetainsTheExistingError() {
        affectedRows = 0;
        assertThrows(IllegalArgumentException.class, () -> service.updatePost(edit()));
    }

    @Test
    public void directHttpEditOfBlindedPostReturns403() throws Exception {
        affectedRows = 0;
        storedPost.setIsBlinded(true);
        var login = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(LoginUser.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                    NativeWebRequest request, WebDataBinderFactory binderFactory) {
                return 7L;
            }
        };
        var mvc = MockMvcBuilders.standaloneSetup(new PostController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).setCustomArgumentResolvers(login).build();
        var response = mvc.perform(put("/api/community/posts/10").contentType(MediaType.APPLICATION_JSON)
                .content("{\"postTitle\":\"edited\",\"postContent\":\"restored\",\"isBlinded\":false}"))
                .andReturn().getResponse();
        assertEquals(403, response.getStatus());
    }

    private PostDTO edit() {
        var post = new PostDTO();
        post.setPostNum(10L);
        post.setUserNum(7L);
        post.setPostTitle("수정 제목");
        post.setPostContent("수정 본문");
        post.setIsBlinded(false);
        return post;
    }
}
