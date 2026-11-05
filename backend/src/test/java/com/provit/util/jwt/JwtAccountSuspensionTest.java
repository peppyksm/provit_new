package com.provit.util.jwt;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.lang.reflect.Proxy;
import java.util.Date;

import jakarta.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.provit.common.auth.AuthCookieService;
import com.provit.controller.auth.AuthController;
import com.provit.dao.auth.RefreshTokenDAO;
import com.provit.dao.auth.UserDAO;
import com.provit.dto.auth.UserDTO;
import com.provit.dto.auth.UserResponseDTO;
import com.provit.service.auth.AuthService;

public class JwtAccountSuspensionTest {
    private UserDTO user;
    private JwtProvider jwt;
    private String accessToken;
    private String refreshToken;
    private MockMvc mvc;
    private int profileReads;
    private int refreshTokenCalls;

    @Before
    public void setUp() {
        user = UserDTO.builder().userNum(7L).userType("USER")
                .userIsDeleted(0).userTokenVersion(0).build();
        UserDAO users = (UserDAO) Proxy.newProxyInstance(UserDAO.class.getClassLoader(),
                new Class<?>[] { UserDAO.class }, (proxy, method, args) -> {
                    if (method.getName().equals("selectByUserNum")) return user;
                    throw new AssertionError("Unexpected user DAO call: " + method.getName());
                });
        jwt = new JwtProvider(users);
        ReflectionTestUtils.setField(jwt, "secretKeyPlain", "account-suspension-test-key-with-at-least-32-bytes");
        ReflectionTestUtils.setField(jwt, "expirationTime", 60000L);
        ReflectionTestUtils.setField(jwt, "refreshExpirationTime", 120000L);
        jwt.init();
        accessToken = jwt.createToken(user);
        refreshToken = jwt.createRefreshToken(user);

        AuthService auth = (AuthService) Proxy.newProxyInstance(AuthService.class.getClassLoader(),
                new Class<?>[] { AuthService.class }, (proxy, method, args) -> {
                    if (method.getName().equals("getUserProfile")) {
                        profileReads++;
                        return UserResponseDTO.from(user);
                    }
                    throw new AssertionError("Unexpected auth service call: " + method.getName());
                });
        RefreshTokenDAO refreshTokens = (RefreshTokenDAO) Proxy.newProxyInstance(
                RefreshTokenDAO.class.getClassLoader(), new Class<?>[] { RefreshTokenDAO.class },
                (proxy, method, args) -> {
                    refreshTokenCalls++;
                    throw new AssertionError("Suspended account reached refresh token storage: " + method.getName());
                });
        AuthCookieService cookies = new AuthCookieService();
        ReflectionTestUtils.setField(cookies, "cookieName", "provit_access");
        ReflectionTestUtils.setField(cookies, "refreshCookieName", "provit_refresh");
        ReflectionTestUtils.setField(cookies, "sameSite", "Lax");
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(auth, jwt, cookies, refreshTokens)).build();
    }

    @Test
    public void activeAccountCanUseBothTokens() {
        assertTrue(jwt.validateToken(accessToken));
        assertTrue(jwt.validateRefreshToken(refreshToken));
    }

    @Test
    public void suspensionImmediatelyRejectsPreviouslyIssuedAccessAndRefreshTokens() {
        user.setBlockedDate(new Date(System.currentTimeMillis() + 86400000L));
        assertFalse(jwt.validateToken(accessToken));
        assertFalse(jwt.validateRefreshToken(refreshToken));
    }

    @Test
    public void expiredSuspensionAllowsBothTokens() {
        user.setBlockedDate(new Date(System.currentTimeMillis() - 60000L));
        assertTrue(jwt.validateToken(accessToken));
        assertTrue(jwt.validateRefreshToken(refreshToken));
    }

    @Test
    public void manualUnblockingRestoresAccessWithoutChangingTokenVersion() {
        user.setBlockedDate(new Date(System.currentTimeMillis() + 86400000L));
        assertFalse(jwt.validateToken(accessToken));
        user.setBlockedDate(null);
        assertTrue(jwt.validateToken(accessToken));
        assertTrue(jwt.validateRefreshToken(refreshToken));
    }

    @Test
    public void unblockingDoesNotRestoreDeletedAccountsOrRevokedTokens() {
        user.setBlockedDate(null);
        user.setUserIsDeleted(1);
        assertFalse(jwt.validateToken(accessToken));
        assertFalse(jwt.validateRefreshToken(refreshToken));
        user.setUserIsDeleted(0);
        user.setUserTokenVersion(1);
        assertFalse(jwt.validateToken(accessToken));
        assertFalse(jwt.validateRefreshToken(refreshToken));
    }

    @Test
    public void suspendedAccountCannotReadProfileWithAnExistingCookie() throws Exception {
        user.setBlockedDate(new Date(System.currentTimeMillis() + 86400000L));
        var response = mvc.perform(get("/api/auth/me").cookie(new Cookie("provit_access", accessToken)))
                .andReturn().getResponse();
        assertEquals(401, response.getStatus());
        assertEquals(0, profileReads);
    }

    @Test
    public void suspendedRefreshClearsBothCookiesWithoutRotatingTokens() throws Exception {
        user.setBlockedDate(new Date(System.currentTimeMillis() + 86400000L));
        var response = mvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("provit_refresh", refreshToken))).andReturn().getResponse();
        assertEquals(401, response.getStatus());
        assertEquals(0, refreshTokenCalls);
        var cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertEquals(2, cookies.size());
        assertTrue(cookies.stream().anyMatch(value -> value.startsWith("provit_access=") && value.contains("Max-Age=0")));
        assertTrue(cookies.stream().anyMatch(value -> value.startsWith("provit_refresh=") && value.contains("Max-Age=0")));
    }

    @Test
    public void unblockedAccountCanReadProfileWithAnExistingCookie() throws Exception {
        user.setBlockedDate(new Date(System.currentTimeMillis() - 60000L));
        var response = mvc.perform(get("/api/auth/me").cookie(new Cookie("provit_access", accessToken)))
                .andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertEquals(1, profileReads);
    }
}
