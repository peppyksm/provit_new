package com.provit.support;

import com.provit.dao.FileOwnershipDAO;
import com.provit.dao.TestDao;
import com.provit.dao.admin.*;
import com.provit.dao.auth.*;
import com.provit.dao.community.*;
import com.provit.dao.document.*;
import com.provit.dao.interview.*;
import com.provit.dao.recruitment.*;
import com.provit.dao.study.*;
import com.provit.persistence.JpaStore;

import jakarta.persistence.EntityManagerFactory;

import org.junit.Assume;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Properties;
import java.util.UUID;

import javax.sql.DataSource;

/** Each test gets a complete schema; application tables are never used or cleared. */
public final class PostgresTestDatabase implements AutoCloseable {
    private final String schema = "provit_test_" + UUID.randomUUID().toString().replace("-", "");
    private Connection admin;
    public final DriverManagerDataSource dataSource;
    public final Connection connection;
    private final AnnotationConfigApplicationContext context;

    public EntityManagerFactory entityManagerFactory() {
        return context.getBean(EntityManagerFactory.class);
    }

    public final JpaStore store;
    public final UserDAO users;
    public final DocumentDAO documents;
    public final MainResumeDAO mainResume;
    public final DocumentReviewDAO reviews;
    public final PostDAO posts;
    public final CommentDAO comments;
    public final RecruitmentDAO recruitment;
    public final StudyDAO studies;
    public final InterviewDAO interview;
    public final RefreshTokenDAO refreshTokens;
    public final AdminReportDAO adminReports;
    public final AdminUserDAO adminUsers;
    public final ReportDAO reports;
    public final FileOwnershipDAO fileOwners;
    public final TestDao health;
    public final JdbcTemplate jdbc;
    public final JpaTransactionManager transactions;

    public static PostgresTestDatabase open() throws Exception {
        Assume.assumeTrue(
                "Opt in with -Dprovit.postgres.tests=true",
                Boolean.getBoolean("provit.postgres.tests"));
        return new PostgresTestDatabase();
    }

    private PostgresTestDatabase() throws Exception {
        var properties = new Properties();
        Path settings = Path.of("src/main/resources/database.properties");
        if (Files.exists(settings)) {
            try (var reader = Files.newBufferedReader(settings, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        }
        String url =
                System.getProperty(
                        "provit.postgres.url",
                        properties.getProperty(
                                "db.url", "jdbc:postgresql://localhost:5432/provit_new"));
        if (!url.startsWith("jdbc:postgresql:"))
            throw new IllegalArgumentException("PostgreSQL test URL required");
        String username =
                System.getProperty(
                        "provit.postgres.username",
                        properties.getProperty("db.username", "provit"));
        String password =
                System.getProperty(
                        "provit.postgres.password", properties.getProperty("db.password", ""));
        var base = new DriverManagerDataSource(url, username, password);
        admin = base.getConnection();
        try {
            try (var statement = admin.createStatement()) {
                statement.execute("CREATE SCHEMA " + schema);
            }
            dataSource =
                    new DriverManagerDataSource(
                            url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema,
                            username,
                            password);
            var options = new Properties();
            options.setProperty("connectTimeout", "5");
            options.setProperty("socketTimeout", "20");
            options.setProperty("options", "-c timezone=Asia/Seoul");
            dataSource.setConnectionProperties(options);
            connection = dataSource.getConnection();
            ScriptUtils.executeSqlScript(
                    connection,
                    new EncodedResource(
                            new ClassPathResource("sql_query/schema.sql"), StandardCharsets.UTF_8));
            context = new AnnotationConfigApplicationContext();
            context.registerBean("dataSource", DataSource.class, () -> dataSource);
            context.register(JpaTestConfiguration.class);
            context.refresh();
            store = context.getBean(JpaStore.class);
            users = context.getBean(UserDAO.class);
            documents = context.getBean(DocumentDAO.class);
            mainResume = context.getBean(MainResumeDAO.class);
            reviews = context.getBean(DocumentReviewDAO.class);
            posts = context.getBean(PostDAO.class);
            comments = context.getBean(CommentDAO.class);
            recruitment = context.getBean(RecruitmentDAO.class);
            studies = context.getBean(StudyDAO.class);
            interview = context.getBean(InterviewDAO.class);
            refreshTokens = context.getBean(RefreshTokenDAO.class);
            adminReports = context.getBean(AdminReportDAO.class);
            adminUsers = context.getBean(AdminUserDAO.class);
            reports = context.getBean(ReportDAO.class);
            fileOwners = context.getBean(FileOwnershipDAO.class);
            health = context.getBean(TestDao.class);
            jdbc = new JdbcTemplate(dataSource);
            transactions = context.getBean(JpaTransactionManager.class);
        } catch (Exception failure) {
            try (var statement = admin.createStatement()) {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
            admin.close();
            throw failure;
        }
    }

    @Override
    public void close() throws Exception {
        try {
            context.close();
            connection.close();
        } finally {
            try (var statement = admin.createStatement()) {
                if (!schema.matches("provit_test_[a-f0-9]{32}"))
                    throw new IllegalStateException("Invalid test schema");
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            } finally {
                admin.close();
            }
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    @ComponentScan(basePackages = {"com.provit.dao", "com.provit.persistence"})
    static class JpaTestConfiguration {
        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.provit.persistence");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(
                    java.util.Map.of(
                            "hibernate.hbm2ddl.auto",
                            "validate",
                            "hibernate.jdbc.time_zone",
                            "Asia/Seoul"));
            return factory;
        }

        @Bean
        JpaTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }

        @Bean
        static PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
            return new PersistenceExceptionTranslationPostProcessor();
        }
    }
}
