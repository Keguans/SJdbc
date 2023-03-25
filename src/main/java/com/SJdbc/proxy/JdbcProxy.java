package com.SJdbc.proxy;

import com.SJdbc.annotation.Param;
import com.SJdbc.annotation.Sql;
import com.SJdbc.config.SpringContextHolder;
import com.SJdbc.enums.SqlEnum;
import com.SJdbc.util.JdbcUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

public class JdbcProxy implements InvocationHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @SuppressWarnings(value = "all")
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Sql sql = method.getAnnotation(Sql.class);
        if (sql.type() == 1) {
            return this.typeOne(sql.sql(), method, args);
        } else {
            return this.typeTwo(sql.sql(), method, args);
        }
    }

    /**
     * 入参类型 1
     *
     * @param method
     * @param args
     * @return
     */
    public Object typeOne(String sqlStr, Method method, Object[] args) throws Throwable {
        for (int i = 1; i <= args.length; i++) {
            Object arg = args[i - 1];
            Object convert = JdbcUtil.convert(arg);
            if (convert instanceof Collection) {
                convert = objectMapper.writeValueAsString(convert)
                        .replace("[", "(")
                        .replace("]", ")");
            }
            sqlStr = sqlStr.replace("?" + i, String.valueOf(convert));
        }
        return this.doSql(sqlStr, method);
    }

    /**
     * 入参类型 2
     *
     * @param sqlStr
     * @param method
     * @param args
     * @return
     * @throws Exception
     */
    public Object typeTwo(String sqlStr, Method method, Object[] args) throws Exception {
        sqlStr = sqlStr.trim();
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            Annotation[] parameterAnnotation = method.getParameterAnnotations()[i];
            String param = parameterAnnotation.length == 0 ? method.getParameters()[i].getName() : ((Param) parameterAnnotation[0]).name();
            Object arg = args[i];
            Map<String, Object> map = objectMapper.readValue(objectMapper.writeValueAsString(arg), new TypeReference<HashMap<String, Object>>() {});
            while (sqlStr.contains(param)) {
                String params = sqlStr.substring(sqlStr.indexOf(param));
                String substring;
                if (!params.contains(" ")) {
                    substring = params;
                } else {
                    substring = params.substring(0, params.indexOf(" "));
                }
                Object obj = map.get(substring.split("\\.")[1]);
                Object convert = JdbcUtil.convert(obj);
                if (convert instanceof Collection) {
                    convert = objectMapper.writeValueAsString(convert)
                            .replace("[", "(")
                            .replace("]", ")");
                }
                sqlStr = sqlStr.replace(substring, String.valueOf(convert));
            }
        }
        return this.doSql(sqlStr, method);
    }

    /**
     * 执行 sql
     *
     * @param sqlStr
     * @param method
     * @return
     * @throws ClassNotFoundException
     */
    private Object doSql(String sqlStr, Method method) throws ClassNotFoundException {
        JdbcTemplate jdbcTemplate = SpringContextHolder.getBean(JdbcTemplate.class);
        if (sqlStr.toUpperCase().startsWith(SqlEnum.SELECT.getWord())) {
            if (List.class.isAssignableFrom(method.getReturnType())) {
                Type genericReturnType = method.getGenericReturnType();
                String typeName = genericReturnType.getTypeName();
                Class<?> aClass = Class.forName(typeName.substring(typeName.indexOf("<") + 1, typeName.indexOf(">")));
                return jdbcTemplate.query(sqlStr, new BeanPropertyRowMapper<>(aClass));
            }
            if (Map.class.isAssignableFrom(method.getReturnType())) {
                return jdbcTemplate.queryForMap(sqlStr);
            }
            try {
                return jdbcTemplate.queryForObject(sqlStr, new BeanPropertyRowMapper<>(method.getReturnType()));
            } catch (EmptyResultDataAccessException e) {
                return null;
            }
        }
        return jdbcTemplate.update(sqlStr);
    }
}
