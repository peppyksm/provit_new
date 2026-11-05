package com.provit.config.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/** Preserves the application's existing empty-string-as-null database contract. */
@MappedTypes(String.class)
@MappedJdbcTypes(value = {JdbcType.CHAR, JdbcType.VARCHAR, JdbcType.LONGVARCHAR}, includeNullJdbcType = true)
public class EmptyStringAsNullTypeHandler extends BaseTypeHandler<String> {
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, String value, JdbcType jdbcType)
            throws SQLException {
        if (value.isEmpty()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    @Override
    public String getNullableResult(ResultSet results, String column) throws SQLException {
        return results.getString(column);
    }

    @Override
    public String getNullableResult(ResultSet results, int column) throws SQLException {
        return results.getString(column);
    }

    @Override
    public String getNullableResult(CallableStatement statement, int column) throws SQLException {
        return statement.getString(column);
    }
}
