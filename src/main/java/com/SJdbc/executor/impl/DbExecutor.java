package com.SJdbc.executor.impl;

import com.SJdbc.executor.cache.CacheExecutor;
import com.SJdbc.proxy.JdbcProxy;

import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * 数据库执行器
 */
public class DbExecutor implements CacheExecutor {

    /**
     * 执行数据查询，由子类实现
     *
     * @param key
     * @param method
     * @return
     */
    protected Object doGetData(String key, Method method, Object... params) throws ClassNotFoundException {
        return null;
    }

    @Override
    public Object getData(Object key) {
        HashMap map = (HashMap) key;
        DbExecutorKey dbExecutorKey = (DbExecutorKey) map.get("DbExecutorKey");
        JdbcProxy.SqlAndParam sqlAndParam = (JdbcProxy.SqlAndParam) map.get("sqlAndParam");
        try {
            return this.doGetData(dbExecutorKey.sql, dbExecutorKey.method, sqlAndParam.getParams().toArray());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void setData(Object key, Object data) {}

    @Override
    public void clear(Object key) {}

    public static class DbExecutorKey {

        public DbExecutorKey(String sql, Method method) {
            this.sql = sql;
            this.method = method;
        }

        private final String sql;

        private final Method method;

        public String getSql() {
            return sql;
        }

        public Method getMethod() {
            return method;
        }
    }
}
