package com.SJdbc.proxy;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.crypto.digest.MD5;
import com.SJdbc.annotation.Cache;
import com.SJdbc.annotation.Param;
import com.SJdbc.annotation.Sql;
import com.SJdbc.config.SpringContextHolder;
import com.SJdbc.enums.SqlEnum;
import com.SJdbc.executor.cache.CacheExecutor;
import com.SJdbc.executor.cache.impl.DefaultCacheExecutor;
import com.SJdbc.executor.impl.DbExecutor;
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
        SqlAndParam sqlAndParam = !sql.useParam() ? this.typeOne(sql.sql(), args) : this.typeTwo(sql.sql(), method, args);
        String sqlStr = sqlAndParam.getSql();
        // 查询方法
        if (sqlStr.toUpperCase().startsWith(SqlEnum.SELECT.getWord())) {
            Cache annotation = method.getDeclaringClass().getAnnotation(Cache.class);
            CacheExecutor executor = this.setCacheExecutor(annotation);
            // 如果未设置缓存，查询数据库
            if (executor instanceof DbExecutor) {
                Map<String, Object> map = new HashMap<>(4);
                map.put("sqlAndParam", sqlAndParam);
                map.put("DbExecutorKey", new DbExecutorKey(sqlStr, method));
                return executor.getData(map);
            }
            // 查询缓存
            String key = this.getKey(sqlStr, method, sqlAndParam.getParams());
            Object data = executor.getData(key);
            if (Objects.isNull(data)) {
                // 缓存没有，查询数据库
                data = this.doGetData(sqlStr, method, sqlAndParam.getParams().toArray());
                executor.setData(key, data);
            }
            return data;
        }
        // 非查询 sql
        return this.doUpdate(sqlStr, method.getReturnType(), sqlAndParam.getParams().toArray());
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
    private String getKey(String sqlStr, Method method, List<Object> params) {
        return MD5.create().digestHex(method.getDeclaringClass() + ":" + method.getName() + ":" + method.getReturnType() + ":" + sqlStr + ":" + params.toString());
    }

    /**
     * 入参类型 1
     *
     * @param args
     * @return
     */
    public SqlAndParam typeOne(String sqlStr, Object[] args) throws Throwable {
        List<Object> paramsList = new LinkedList<>();
        if (Objects.isNull(args)) {
            return new SqlAndParam(sqlStr, paramsList);
        }
        while (true) {
            int indexOf = sqlStr.indexOf("?");
            if (indexOf <= 0) {
                break;
            }
            String substring = sqlStr.substring(indexOf);
            String num;
            if (!substring.contains(" ")) {
                num = substring.substring(1);
            } else {
                num = substring.substring(1, substring.indexOf(" "));
            }
            Object arg = args[Integer.parseInt(num) - 1];
            StringBuilder seat = new StringBuilder("^~");
            if (arg instanceof Collection) {
                Collection c = (Collection) arg;
                paramsList.addAll(c);
                seat = new StringBuilder("(");
                for (int i = 0; i < c.size(); i++) {
                    seat.append("^~, ");
                }
                seat = new StringBuilder(seat.substring(0, seat.length() - 2) + ")");
            } else {
                paramsList.add(args[Integer.parseInt(num) - 1]);
            }
            if (!substring.contains(" ")) {
                sqlStr = sqlStr.substring(0, indexOf) + seat;
            } else {
                sqlStr = sqlStr.substring(0, indexOf) + seat + substring.substring(substring.indexOf(" "));
            }
        }
        sqlStr = sqlStr.replace("^~", "?");
        return new SqlAndParam(sqlStr, paramsList);
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
    public SqlAndParam typeTwo(String sqlStr, Method method, Object[] args) throws Exception {
        sqlStr = sqlStr.trim();
        List<Object> paramsList = new LinkedList<>();
        if (Objects.isNull(args)) {
            return new SqlAndParam(sqlStr, paramsList);
        }
        Map<String, Object> paramMap = new HashMap<>();
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            Annotation[] parameterAnnotation = method.getParameterAnnotations()[i];
            String param = parameterAnnotation.length == 0 ? method.getParameters()[i].getName() : ((Param) parameterAnnotation[0]).name();
            Map<String, Object> stringObjectHashMap = objectMapper.readValue(objectMapper.writeValueAsString(args[i]), new TypeReference<HashMap<String, Object>>() {});
            paramMap.put(param, stringObjectHashMap);
        }
        while (true) {
            int indexOf = sqlStr.indexOf(".");
            if (indexOf <= 0) {
                break;
            }
            String param = sqlStr.substring(sqlStr.substring(0, indexOf).lastIndexOf(" ") + 1, indexOf);
            Map paramObj = (HashMap) paramMap.get(param);
            String val;
            if (!sqlStr.substring(indexOf).contains(" ")) {
                val = sqlStr.substring(indexOf + 1);
            } else {
                val = sqlStr.substring(indexOf + 1, indexOf + sqlStr.substring(indexOf).indexOf(" "));
            }
            Object o = paramObj.get(val);
            StringBuilder seat = new StringBuilder("^~");
            if (o instanceof Collection) {
                seat = new StringBuilder("(");
                Collection collection = (Collection) o;
                for (int i = 0; i < collection.size(); i++) {
                    seat.append("^~, ");
                }
                seat = new StringBuilder(seat.substring(0, seat.length() - 2) + ")");
                paramsList.addAll(collection);
            } else {
                paramsList.add(o);
            }
            String substring = sqlStr.substring(indexOf);
            if (!substring.contains(" ")) {
                sqlStr = sqlStr.substring(0, indexOf - param.length()) + seat;
            } else {
                sqlStr = sqlStr.substring(0, indexOf - param.length()) + seat + substring.substring(substring.indexOf(" "));
            }
        }
        sqlStr = sqlStr.replace("^~", "?");
        return new SqlAndParam(sqlStr, paramsList);
    }

    public static class SqlAndParam {

        public SqlAndParam(String sql, List<Object> params) {
            this.sql = sql;
            this.params = params;
        }

        private final String sql;

        private final List<Object> params;

        public String getSql() {
            return sql;
        }

        public List<Object> getParams() {
            return params;
        }
    }

    /**
     * 执行 sql
     *
     * @param sqlStr
     * @param method
     * @param params
     * @return
     */
    @Override
    protected Object doGetData(String sqlStr, Method method, Object... params) throws ClassNotFoundException {
        JdbcTemplate jdbcTemplate = SpringContextHolder.getBean(JdbcTemplate.class);
        if (List.class.isAssignableFrom(method.getReturnType())) {
            Type genericReturnType = method.getGenericReturnType();
            String typeName = genericReturnType.getTypeName();
            Class<?> aClass = Class.forName(typeName.substring(typeName.indexOf("<") + 1, typeName.indexOf(">")));
            return jdbcTemplate.query(sqlStr, new BeanPropertyRowMapper<>(aClass), params);
        }
        try {
            if (Map.class.isAssignableFrom(method.getReturnType())) {
                return jdbcTemplate.queryForMap(sqlStr, params);
            }
            if (ClassUtil.isBasicType(method.getReturnType()) || Arrays.asList(String.class, Date.class).contains(method.getReturnType())) {
                return jdbcTemplate.queryForObject(sqlStr, method.getReturnType(), params);
            }
            return jdbcTemplate.queryForObject(sqlStr, new BeanPropertyRowMapper<>(method.getReturnType()), params);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * 执行非查询 sql
     *
     * @param sqlStr
     * @param returnType
     * @param params
     * @return
     */
    private Object doUpdate(String sqlStr, Class<?> returnType, Object... params) {
        // 获取 jdbcTemplate
        JdbcTemplate jdbcTemplate = SpringContextHolder.getBean(JdbcTemplate.class);
        // 执行 sql
        int count = jdbcTemplate.update(sqlStr, params);
        // 清空缓存
        if (Objects.nonNull(cacheExecutor)) {
            cacheExecutor.clear(sqlStr);
        }
        if (returnType.equals(Long.class)) {
            return Long.parseLong(String.valueOf(count));
        }
        return count;
    }
}
