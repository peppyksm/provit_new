package com.provit.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.lang.reflect.Proxy;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.provit.common.ApiResponseStatusAdvice;
import com.provit.common.GlobalExceptionHandler;
import com.provit.common.annotation.LoginUser;
import com.provit.common.auth.AuthCookieService;
import com.provit.controller.admin.*;
import com.provit.controller.community.*;
import com.provit.controller.recruitment.RecruitmentPostController;
import com.provit.controller.study.StudyController;
import com.provit.dao.admin.AdminReportDAO;
import com.provit.dto.common.PageResponseDTO;
import com.provit.service.admin.AdminReportService;
import com.provit.service.admin.AdminUserService;
import com.provit.service.study.StudyService;

public class AuthHttpStatusTest {
    private MockMvc mvc;
    private Long loginUser;
    private boolean admin;

    @Before
    public void setUp() {
        var reports = new AdminReportController();
        ReflectionTestUtils.setField(reports, "adminReportService", proxy(AdminReportService.class));
        var login = new HandlerMethodArgumentResolver() {
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(LoginUser.class);
            }
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                    NativeWebRequest request, WebDataBinderFactory factory) { return loginUser; }
        };
        mvc = MockMvcBuilders.standaloneSetup(new PostController(null), new CommentController(null),
                new MyCommentController(null), new ReportController(), new StudyController(proxy(StudyService.class)),
                reports, new AdminUserController(proxy(AdminUserService.class), proxy(AdminReportDAO.class)),
                new RecruitmentPostController(null, null, new AuthCookieService()))
                .setControllerAdvice(new ApiResponseStatusAdvice(), new GlobalExceptionHandler())
                .setCustomArgumentResolvers(login).build();
    }

    @Test
    public void anonymousProtectedRequestsReturn401WithTheExistingJsonContract() throws Exception {
        String[][] requests = {
                {"GET", "/api/community/posts/me"}, {"POST", "/api/community/posts"},
                {"PUT", "/api/community/posts/1"}, {"DELETE", "/api/community/posts/1"},
                {"POST", "/api/community/posts/1/like"}, {"GET", "/api/community/comments/me"},
                {"POST", "/api/community/posts/1/comments"}, {"PUT", "/api/community/posts/1/comments/1"},
                {"DELETE", "/api/community/posts/1/comments/1"}, {"POST", "/api/community/report"},
                {"POST", "/api/studies"}, {"PUT", "/api/studies/1"}, {"DELETE", "/api/studies/1"},
                {"POST", "/api/studies/1/join"}, {"DELETE", "/api/studies/1/leave"},
                {"GET", "/api/admin/reports"}, {"PUT", "/api/admin/reports/1"},
                {"GET", "/api/admin/users"}, {"PUT", "/api/admin/users/1/status"},
                {"POST", "/api/recruitment/sync"}, {"POST", "/api/recruitment/expire"}
        };
        for (var request : requests) {
            var response = mvc.perform(request(org.springframework.http.HttpMethod.valueOf(request[0]), request[1])
                    .contentType(MediaType.APPLICATION_JSON).content("{}")) .andReturn().getResponse();
            assertEquals(request[0] + " " + request[1], 401, response.getStatus());
            assertEquals(318, new ObjectMapper().readTree(response.getContentAsByteArray())
                    .path("responseCode").path("code").asInt());
        }
    }

    @Test
    public void authenticatedNonAdminsReceive403InsteadOfRefreshingTheirLogin() throws Exception {
        loginUser = 7L;
        for (String path : List.of("/api/admin/reports", "/api/admin/users")) {
            assertEquals(403, mvc.perform(get(path)).andReturn().getResponse().getStatus());
        }
        for (String path : List.of("/api/admin/reports/1", "/api/admin/users/1/status")) {
            var response = mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andReturn().getResponse();
            assertEquals(403, response.getStatus());
            assertEquals(322, new ObjectMapper().readTree(response.getContentAsByteArray())
                    .path("responseCode").path("code").asInt());
        }
    }

    @Test
    public void anonymousPublicStudyListStillReturns200() throws Exception {
        assertEquals(200, mvc.perform(get("/api/studies")).andReturn().getResponse().getStatus());
    }

    @Test
    public void authorizedAdminListsStillReturn200() throws Exception {
        loginUser = 7L;
        admin = true;
        assertEquals(200, mvc.perform(get("/api/admin/reports")).andReturn().getResponse().getStatus());
        assertEquals(200, mvc.perform(get("/api/admin/users")).andReturn().getResponse().getStatus());
    }

    private <T> T proxy(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                (proxy, method, args) -> switch (method.getName()) {
                    case "isAdmin" -> admin;
                    case "getReportList", "getUserList" -> new PageResponseDTO<>(List.of(), 0, 1, 10);
                    case "getStudyList" -> List.of();
                    default -> throw new AssertionError("Unexpected service call: " + method.getName());
                }));
    }
}
