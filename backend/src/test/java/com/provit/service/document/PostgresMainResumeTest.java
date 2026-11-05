package com.provit.service.document;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.*;
import org.junit.rules.TestName;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.provit.common.GlobalExceptionHandler;
import com.provit.common.auth.AuthCookieService;
import com.provit.common.resolver.LoginUserArgumentResolver;
import com.provit.controller.document.MainResumeController;
import com.provit.dao.auth.UserDAO;
import com.provit.dao.document.MainResumeDAO;
import com.provit.dao.document.impl.MainResumeDAOImpl;
import com.provit.dto.auth.UserDTO;
import com.provit.dto.document.MainResumeJobInfoDTO;
import com.provit.service.document.impl.MainResumeServiceImpl;
import com.provit.util.jwt.JwtProvider;

public class PostgresMainResumeTest {
    @Rule
    public final TestName testName = new TestName();
    private final String prefix = "MRT" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    private final Map<String, String> names = new LinkedHashMap<>();
    private final List<String> tables = new ArrayList<>();
    private Connection connection;
    private com.provit.support.PostgresTestDatabase database;
    private SqlSessionTemplate session;
    private MainResumeDAO dao;
    private MainResumeService service;
    private DataSourceTransactionManager transactions;

    @Before
    public void setup() throws Exception {
        database = com.provit.support.PostgresTestDatabase.open();
        connection = database.connection;
        var source = database.dataSource;
        execute("INSERT INTO T_OCCUPATION VALUES ('2','IT개발·데이터')");
        execute("INSERT INTO T_JOB VALUES ('84','2','백엔드/서버개발')");
        for (int userNum : List.of(7, 8)) {
            execute("INSERT INTO T_USER (USER_NUM,USER_NAME,USER_NICKNAME,USER_EMAIL,USER_PW,OCCUPATION_CODE,JOB_CODE) VALUES ("
                    + userNum + ",'test','test" + userNum + "','test" + userNum + "@example.invalid','placeholder',NULL,NULL)");
        }
        execute("INSERT INTO T_RESUME (RESUME_NUM,USER_NUM,RESUME_TITLE,HIGHEST_LEVEL,OCCUPATION_CODE,JOB_CODE) VALUES (11,7,'first','대학교','2','84')");
        execute("INSERT INTO T_RESUME (RESUME_NUM,USER_NUM,RESUME_TITLE,HIGHEST_LEVEL) VALUES (3000000000,7,'second','대학교')");
        execute("INSERT INTO T_RESUME (RESUME_NUM,USER_NUM,RESUME_TITLE,HIGHEST_LEVEL) VALUES (22,8,'private','대학교')");
        session = database.session;
        dao = new MainResumeDAOImpl(session);
        transactions = new DataSourceTransactionManager(source);
        service = transactional(dao);
    }

    @After
    public void cleanup() throws Exception {
        if (database != null) database.close();
    }

    @Test
    public void authenticatedApiPersistsOnlyOwnedResumeAndReturnsJobCodes() throws Exception {
        UserDAO users = (UserDAO) java.lang.reflect.Proxy.newProxyInstance(UserDAO.class.getClassLoader(),
                new Class<?>[] { UserDAO.class }, (proxy, method, args) ->
                    session.selectOne("com.provit.mapper.UserMapper.selectByUserNum", args[0]));
        var jwt = new JwtProvider(users);
        ReflectionTestUtils.setField(jwt, "secretKeyPlain", "main-resume-postgres-test-key-32-bytes-long");
        ReflectionTestUtils.setField(jwt, "expirationTime", 60_000L);
        jwt.init();
        var cookies = new AuthCookieService();
        ReflectionTestUtils.setField(cookies, "cookieName", "provit_access");
        var cookie = new jakarta.servlet.http.Cookie("provit_access", jwt.createToken(users.selectByUserNum(7L)));
        var mvc = MockMvcBuilders.standaloneSetup(new MainResumeController(service))
                .setCustomArgumentResolvers(new LoginUserArgumentResolver(jwt, cookies))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        var json = new ObjectMapper();
        var result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/documents/main-resume").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"resumeNum\":11,\"userNum\":8}")).andReturn().getResponse();
        assertEquals(200, result.getStatus());
        assertEquals(Long.valueOf(11), storedMainResumeNum());
        var data = json.readTree(result.getContentAsString(StandardCharsets.UTF_8)).path("data");
        assertEquals("2", data.path("occupationCode").asText());
        assertEquals("84", data.path("jobCode").asText());
        assertEquals("백엔드/서버개발", data.path("jobName").asText());
        result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/documents/main-resume").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"resumeNum\":22}")).andReturn().getResponse();
        assertEquals(404, result.getStatus());
        assertEquals(Long.valueOf(11), storedMainResumeNum());
        result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/documents/main-resume").cookie(cookie)).andReturn().getResponse();
        assertEquals(200, result.getStatus());
        assertEquals(11, json.readTree(result.getContentAsString(StandardCharsets.UTF_8)).path("data").path("resumeNum").asLong());
        result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/documents/main-resume").cookie(cookie)).andReturn().getResponse();
        assertEquals(200, result.getStatus());
        assertNull(storedMainResumeNum());
    }

    @Test
    public void newSchemaSupportsDesignationAndDeleteReset() throws Exception {
        assertNull(storedMainResumeNum());
        assertNull(service.getMainResumeJobInfo(7));
        service.setMainResume(7, 11);
        assertEquals(Long.valueOf(11), storedMainResumeNum());
        execute("DELETE FROM T_RESUME WHERE RESUME_NUM=11");
        assertNull(storedMainResumeNum());
        assertNull(service.getMainResumeJobInfo(7));
    }

    @Test
    public void designationChangeLiveCodesAndDeleteResetWorkWithUserMapping() throws Exception {
        assertNull(service.getMainResumeJobInfo(7));
        var result = service.setMainResume(7, 11);
        assertEquals("2", result.getOccupationCode());
        assertEquals("84", result.getJobCode());
        assertEquals("IT개발·데이터", result.getOccupationName());
        assertEquals("백엔드/서버개발", result.getJobName());
        UserDTO user = session.selectOne("com.provit.mapper.UserMapper.selectByUserNum", 7L);
        assertEquals(Long.valueOf(11), storedMainResumeNum());
        assertNull(user.getJobCode());
        execute("UPDATE T_RESUME SET RESUME_TITLE='edited', JOB_CODE=NULL WHERE RESUME_NUM=11");
        assertEquals("edited", service.getMainResumeJobInfo(7).getResumeTitle());
        assertNull(service.getMainResumeJobInfo(7).getJobCode());
        result = service.setMainResume(7, 3_000_000_000L);
        assertEquals(Long.valueOf(3_000_000_000L), result.getResumeNum());
        assertNull(result.getOccupationCode());
        assertNull(result.getJobName());
        service.setMainResume(7, 3_000_000_000L);
        execute("DELETE FROM T_RESUME WHERE RESUME_NUM=3000000000");
        assertNull(service.getMainResumeJobInfo(7));
        assertNull(storedMainResumeNum());
        service.clearMainResume(7);
        service.clearMainResume(7);
    }

    @Test
    public void foreignMissingAndWithdrawnUsersCannotChangeDesignation() throws Exception {
        service.setMainResume(7, 11);
        for (long resume : new long[] {22, 999}) {
            assertThrows(NoSuchElementException.class, () -> service.setMainResume(7, resume));
            assertEquals(Long.valueOf(11), service.getMainResumeJobInfo(7).getResumeNum());
        }
        execute("UPDATE T_USER SET MAIN_RESUME_NUM=22 WHERE USER_NUM=7");
        assertNull(service.getMainResumeJobInfo(7));
        execute("UPDATE T_USER SET USER_IS_DELETED=1 WHERE USER_NUM=7");
        assertNull(service.getMainResumeJobInfo(7));
        assertThrows(NoSuchElementException.class, () -> service.setMainResume(7, 11));
        assertThrows(NoSuchElementException.class, () -> service.clearMainResume(7));
    }

    @Test
    public void failedResponseReadRollsBackDesignation() {
        service.setMainResume(7, 11);
        var failing = new MainResumeDAO() {
            public int updateMainResume(long userNum, long resumeNum) { return dao.updateMainResume(userNum, resumeNum); }
            public int clearMainResume(long userNum) { return dao.clearMainResume(userNum); }
            public MainResumeJobInfoDTO selectMainResumeJobInfo(long userNum) { return null; }
        };
        assertThrows(NoSuchElementException.class, () -> transactional(failing).setMainResume(7, 3_000_000_000L));
        assertEquals(Long.valueOf(11), service.getMainResumeJobInfo(7).getResumeNum());
    }

    private MainResumeService transactional(MainResumeDAO target) {
        var factory = new ProxyFactory(new MainResumeServiceImpl(target));
        factory.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        return (MainResumeService) factory.getProxy();
    }

    private Long storedMainResumeNum() throws Exception {
        try (var statement = connection.createStatement();
                var rows = statement.executeQuery("SELECT MAIN_RESUME_NUM FROM " + "T_USER" + " WHERE USER_NUM=7")) {
            assertTrue(rows.next());
            long number = rows.getLong(1);
            return rows.wasNull() ? null : number;
        }
    }

    private void execute(String sql) throws Exception {
        try (var statement = connection.createStatement()) { statement.execute(isolated(sql)); }
    }

    private String isolated(String text) { return text; }

    private String resource(String path) throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
