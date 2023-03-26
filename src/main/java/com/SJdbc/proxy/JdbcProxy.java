package com.SJdbc.proxy;

import cn.hutool.crypto.digest.MD5;
import com.SJdbc.annotation.Cache;
import com.SJdbc.annotation.Param;
import com.SJdbc.annotation.Sql;
import com.SJdbc.config.SpringContextHolder;
import com.SJdbc.enums.SqlEnum;
import com.SJdbc.executor.cache.CacheExecutor;
import com.SJdbc.executor.cache.impl.DefaultCacheExecutor;
import com.SJdbc.executor.impl.DbExecutor;
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

public class JdbcProxy extends DbExecutor implements InvocationHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CacheExecutor cacheExecutor = null;

    @Override
    @SuppressWarnings(value = "all")
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Sql sql = method.getAnnotation(Sql.class);
        String sqlStr = sql.type() == 1 ? this.typeOne(sql.sql(), args) : this.typeTwo(sql.sql(), method, args);
        // 查询方法
        if (sqlStr.toUpperCase().startsWith(SqlEnum.SELECT.getWord())) {
            Cache annotation = method.getDeclaringClass().getAnnotation(Cache.class);
            CacheExecutor executor = this.setCacheExecutor(annotation);
            // 如果未设置缓存，查询数据库
            if (executor instanceof DbExecutor) {
                return executor.getData(new DbExecutorKey(sqlStr, method));
            }
            // 查询缓存
            String key = this.getKey(sqlStr, method);
            Object data = executor.getData(key);
            if (Objects.isNull(data)) {
                // 缓存没有，查询数据库
                data = this.doGetData(sqlStr, method);
                executor.setData(key, data);
            }
            return data;
        }
        // 非查询 sql
        return this.doUpdate(sqlStr);
    }

    /**
     * get CacheExecutor
     *
     * @param annotation
     * @return
     */
    private CacheExecutor setCacheExecutor(Cache annotation) {
        if (Objects.nonNull(this.cacheExecutor)) {
            return this.cacheExecutor;
        }
        synchronized (this) {
            if (Objects.isNull(this.cacheExecutor)) {
                if (Objects.isNull(annotation)) {
                    cacheExecutor = this;
                } else {
                    cacheExecutor = annotation.exec().equals(DefaultCacheExecutor.class) ?
                            new DefaultCacheExecutor(annotation.expireTime()) :
                            (CacheExecutor) SpringContextHolder.getBean(annotation.exec());
                }
            }
            return cacheExecutor;
        }
    }

    /**
     * get cache key
     *
     * @param sqlStr
     * @param method
     * @return
     */
    private String getKey(String sqlStr, Method method) {
        return MD5.create().digestHex(method.getDeclaringClass() + ":" + method.getName() + ":" + method.getReturnType() + ":" + sqlStr);
    }

    /**
     * 入参类型 1
     *
     * @param args
     * @return
     */
    public String typeOne(String sqlStr, Object[] args) throws Throwable {
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
        return sqlStr;
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
    public String typeTwo(String sqlStr, Method method, Object[] args) throws Exception {
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
        return sqlStr;
    }

    /**
     * 执行 sql
     *
     * @param sqlStr
     * @param method
     * @return
     */
    @Override
    protected Object doGetData(String sqlStr, Method method) throws ClassNotFoundException {
        JdbcTemplate jdbcTemplate = SpringContextHolder.getBean(JdbcTemplate.class);
        if (List.class.isAssignableFrom(method.getReturnType())) {
            Type genericReturnType = method.getGenericReturnType();
            String typeName = genericReturnType.getTypeName();
            Class<?> aClass = Class.forName(typeName.substring(typeName.indexOf("<") + 1, typeName.indexOf(">")));
            return jdbcTemplate.query(sqlStr, new BeanPropertyRowMapper<>(aClass));
        }
        try {
            if (Map.class.isAssignableFrom(method.getReturnType())) {
                return jdbcTemplate.queryForMap(sqlStr);
            }
            return jdbcTemplate.queryForObject(sqlStr, new BeanPropertyRowMapper<>(method.getReturnType()));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * 执行非查询 sql
     *
     * @param sqlStr
     * @return
     */
    private Object doUpdate(String sqlStr) {
        // 执行 sql
        JdbcTemplate jdbcTemplate = SpringContextHolder.getBean(JdbcTemplate.class);
        int count = jdbcTemplate.update(sqlStr);
        // 清空缓存
        cacheExecutor.clear(sqlStr);
        return count;
    }
}
