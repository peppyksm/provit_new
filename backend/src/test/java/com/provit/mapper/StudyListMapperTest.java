package com.provit.mapper;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.junit.Before;
import org.junit.Test;

public class StudyListMapperTest {
    private MappedStatement statement;

    @Before
    public void loadMapper() throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mappers/study/StudyMapper.xml";
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream);
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        statement = configuration.getMappedStatement("com.provit.dao.study.StudyDAO.selectStudyList");
    }

    @Test
    public void authenticatedListBindsTheCurrentUsersLongId() throws Exception {
        for (Long userNum : List.of(7L, 8L, 3000000000L)) {
            var sql = statement.getBoundSql(userNum);
            assertTrue(sql.getSql().contains("CASE WHEN EXISTS"));
            List<Long> boundUsers = new ArrayList<>();
            PreparedStatement query = (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(), new Class<?>[] { PreparedStatement.class },
                    (proxy, method, args) -> {
                        if (method.getName().equals("setLong")) {
                            assertEquals(1, args[0]);
                            boundUsers.add((Long) args[1]);
                            return null;
                        }
                        throw new AssertionError("Unexpected JDBC call: " + method.getName());
                    });
            new DefaultParameterHandler(statement, userNum, sql).setParameters(query);
            assertEquals(List.of(userNum), boundUsers);
        }
    }

    @Test
    public void anonymousListDoesNotRequireAUserId() {
        var sql = statement.getBoundSql(null);
        assertTrue(sql.getSql().contains("0 AS isJoined"));
        assertFalse(sql.getSql().contains("CASE WHEN EXISTS"));
        assertTrue(sql.getParameterMappings().isEmpty());
    }
}
