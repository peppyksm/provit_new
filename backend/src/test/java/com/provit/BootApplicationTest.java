package com.provit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import com.provit.persistence.entity.UserEntity;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.provit.dao.TestDao;
import com.provit.dao.auth.UserDAO;
import com.provit.dto.auth.LoginResponseDTO;
import com.provit.dto.auth.UserDTO;
import com.provit.dto.community.PostDTO;
import com.provit.dto.document.PortfolioCreateRequestDTO;
import com.provit.dto.document.PortfolioDTO;
import com.provit.service.community.PostService;
import com.provit.service.document.DocumentService;
import com.provit.util.jwt.JwtProvider;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.import=", "provit.scheduling.enabled=false",
        "jwt.secret=boot-migration-test-only-secret-32-bytes-minimum",
        "auth.cookie.secure=false", "auth.cookie.same-site=Lax",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false"
})
class BootApplicationTest {
    @TempDir static Path storage;

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry properties) {
        properties.add("file.upload.base-dir", () -> storage.resolve("public").toString());
        properties.add("portfolio.upload.path", () -> storage.resolve("private").toString());
    }

    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    @Autowired JwtProvider jwt;
    @Autowired EntityManagerFactory entities;
    @Autowired ApplicationContext context;
    @MockitoBean DataSource dataSource;
    @MockitoBean TestDao healthDao;
    @MockitoBean UserDAO users;
    @MockitoBean PostService posts;
    @MockitoBean DocumentService documents;

    private final HttpClient http = HttpClient.newHttpClient();
    private String cookie;

    @BeforeEach
    void authenticateFixture() {
        UserDTO user = UserDTO.builder().userNum(7L).userType("USER")
                .userTokenVersion(0).userIsDeleted(0).build();
        when(users.selectByUserNum(7L)).thenReturn(user);
        cookie = "provit_access=" + jwt.createToken(user);
    }

    @Test
    void embeddedServerExposesExistingHealthJson() throws Exception {
        when(healthDao.selectCurrentTime()).thenReturn("test-db-time");
        var response = send(request("/api/health").GET());
        assertEquals(200, response.statusCode());
        var body = json.readTree(response.body());
        assertEquals(200, body.path("responseCode").path("code").asInt());
        assertEquals("Spring Boot 4.1.2", body.path("data").path("framework").asText());
        assertEquals("test-db-time", body.path("data").path("dbTime").asText());
    }

    @Test
    void bootWiresEntitiesAndJpaTransactionProxies() {
        assertEquals(27,entities.getMetamodel().getEntities().size());
        assertNotNull(entities.getMetamodel().entity(UserEntity.class));
        assertInstanceOf(JpaTransactionManager.class,context.getBean("transactionManager"));
        assertTrue(AopUtils.isAopProxy(context.getBean("documentReviewServiceImpl")));
        assertFalse(context.containsBean("recruitmentScheduler"));
    }

    @Test
    void corsPreflightPreservesCredentialedReactRequests() throws Exception {
        var response = send(request("/api/community/posts")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()));
        assertEquals(200, response.statusCode());
        assertEquals("http://localhost:5173", response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
        assertEquals("true", response.headers().firstValue("Access-Control-Allow-Credentials").orElseThrow());
    }

    @Test
    void anonymousProtectedRequestRetains401() throws Exception {
        var response = send(request("/api/community/posts/me").GET());
        assertEquals(401, response.statusCode());
        verifyNoInteractions(posts);
    }

    @Test
    void realCookieResolverAndJacksonRequestBindingUseAuthenticatedUser() throws Exception {
        when(posts.createPost(any())).thenReturn(42L);
        var response = send(request("/api/community/posts").header("Cookie", cookie)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"postTitle\":\"한글 제목\",\"userNum\":999}")));
        assertEquals(200, response.statusCode(), response.body());
        assertEquals(42L, json.readTree(response.body()).path("data").asLong());
        var post = ArgumentCaptor.forClass(PostDTO.class);
        verify(posts).createPost(post.capture());
        assertEquals(7L, post.getValue().getUserNum());
        assertEquals("한글 제목", post.getValue().getPostTitle());
    }

    @Test
    void reflectedRequestParameterAndJacksonDateFormatRemainCompatible() throws Exception {
        var post = new PostDTO();
        post.setPostTitle("날짜 확인");
        post.setPostDate(new Date(0));
        when(posts.getPopularPosts(3)).thenReturn(List.of(post));
        var response = send(request("/api/community/posts/popular?limit=3").GET());
        assertEquals(200, response.statusCode(), response.body());
        assertEquals("1970-01-01 09:00:00", json.readTree(response.body()).path("data").get(0).path("postDate").asText());
        verify(posts).getPopularPosts(3);
        var login = new LoginResponseDTO();
        login.setAccessToken("private-access");
        login.setRefreshToken("private-refresh");
        assertFalse(json.writeValueAsString(login).contains("private-"));
        assertEquals("0", json.writeValueAsString(new Date(0)));
    }

    @Test
    void publicImagesWorkAndPrivatePortfolioHasNoPublicRoute() throws Exception {
        Path publicRoot = storage.resolve("public");
        Files.write(Files.createDirectories(publicRoot.resolve("post_uploadfile")).resolve("boot.png"), new byte[] { 1 });
        Files.write(Files.createDirectories(publicRoot.resolve("profile_uploadfile")).resolve("boot.png"), new byte[] { 2 });
        Files.write(Files.createDirectories(publicRoot.resolve("portfolio_uploadfile")).resolve("private.pdf"), new byte[] { 3 });
        assertEquals(200, send(request("/uploads/post_uploadfile/boot.png").GET()).statusCode());
        assertEquals(200, send(request("/uploads/profile_uploadfile/boot.png").GET()).statusCode());
        assertEquals(404, send(request("/uploads/portfolio_uploadfile/private.pdf").GET()).statusCode());
        assertEquals(404, send(request("/uploads/profile_uploadfile/missing.png").GET()).statusCode());
    }

    @Test
    void embeddedMultipartParserBindsFileAndUtf8Title() throws Exception {
        var receivedFile = new java.util.concurrent.atomic.AtomicReference<byte[]>();
        when(documents.createPortfolio(eq(7), any())).thenAnswer(call -> {
            PortfolioCreateRequestDTO input = call.getArgument(1);
            receivedFile.set(input.getFile().getBytes());
            return new PortfolioDTO();
        });
        var response = sendMultipart("%PDF-1.7\nfixture".getBytes(StandardCharsets.UTF_8));
        assertEquals(201, response.statusCode(), response.body());
        var input = ArgumentCaptor.forClass(PortfolioCreateRequestDTO.class);
        verify(documents).createPortfolio(eq(7), input.capture());
        assertEquals("한글 포트폴리오", input.getValue().getPortfolioTitle());
        assertArrayEquals("%PDF-1.7\nfixture".getBytes(StandardCharsets.UTF_8), receivedFile.get());
    }

    @Test
    void embeddedMultipartParserRejectsFilesOverExisting20MbLimit() throws Exception {
        var response = sendMultipart(new byte[20 * 1024 * 1024 + 1]);
        assertEquals(400, response.statusCode(), response.body());
        verifyNoInteractions(documents);
    }

    private HttpResponse<String> sendMultipart(byte[] file) throws Exception {
        String boundary = "provit-boot-fixture";
        var body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"portfolioTitle\"\r\n\r\n한글 포트폴리오\r\n"
                + "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"portfolio.pdf\"\r\n"
                + "Content-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(file);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return send(request("/api/documents/portfolios").header("Cookie", cookie)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())));
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(java.time.Duration.ofSeconds(20));
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
