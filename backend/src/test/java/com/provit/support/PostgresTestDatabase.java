package com.provit.support;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Properties;
import java.util.UUID;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Assume;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/** Each test gets a complete schema; application tables are never used or cleared. */
public final class PostgresTestDatabase implements AutoCloseable {
    private final String schema = "provit_test_" + UUID.randomUUID().toString().replace("-", "");
    private Connection admin;
    public final DriverManagerDataSource dataSource;
    public final Connection connection;
    public final SqlSessionFactory factory;
    public final SqlSessionTemplate session;
    public final JdbcTemplate jdbc;
    public final DataSourceTransactionManager transactions;

    public static PostgresTestDatabase open() throws Exception {
        Assume.assumeTrue("Opt in with -Dprovit.postgres.tests=true", Boolean.getBoolean("provit.postgres.tests"));
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
        String url = System.getProperty("provit.postgres.url", properties.getProperty("db.url", "jdbc:postgresql://localhost:5432/provit_new"));
        if (!url.startsWith("jdbc:postgresql:")) throw new IllegalArgumentException("PostgreSQL test URL required");
        String username = System.getProperty("provit.postgres.username", properties.getProperty("db.username", "provit"));
        String password = System.getProperty("provit.postgres.password", properties.getProperty("db.password", ""));
        var base = new DriverManagerDataSource(url, username, password);
        admin = base.getConnection();
        try {
            try (var statement = admin.createStatement()) { statement.execute("CREATE SCHEMA " + schema); }
            dataSource = new DriverManagerDataSource(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema, username, password);
            var options = new Properties();
            options.setProperty("connectTimeout", "5");
            options.setProperty("socketTimeout", "20");
            options.setProperty("options", "-c timezone=Asia/Seoul");
            dataSource.setConnectionProperties(options);
            connection = dataSource.getConnection();
            ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("sql_query/schema.sql"), StandardCharsets.UTF_8));
            var builder = new SqlSessionFactoryBean();
            builder.setDataSource(dataSource);
            builder.setConfigLocation(new ClassPathResource("mybatis-config.xml"));
            builder.setTypeAliasesPackage("com.provit.dto");
            builder.setTypeHandlersPackage("com.provit.config.mybatis");
            builder.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mappers/**/*.xml"));
            factory = builder.getObject();
            session = new SqlSessionTemplate(factory);
            jdbc = new JdbcTemplate(dataSource);
            transactions = new DataSourceTransactionManager(dataSource);
        } catch (Exception failure) {
            try (var statement = admin.createStatement()) { statement.execute("DROP SCHEMA " + schema + " CASCADE"); }
            admin.close();
            throw failure;
        }
    }

    @Override
    public void close() throws Exception {
        try {
            connection.close();
        } finally {
            try (var statement = admin.createStatement()) {
                if (!schema.matches("provit_test_[a-f0-9]{32}")) throw new IllegalStateException("Invalid test schema");
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            } finally {
                admin.close();
            }
        }
    }
}
