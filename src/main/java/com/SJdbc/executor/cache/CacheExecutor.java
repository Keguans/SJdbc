package com.SJdbc.executor.cache;

import com.SJdbc.executor.Executor;

/**
 * 缓存执行器
 */
public interface CacheExecutor extends Executor {

    /**
     * 缓存赋值
     *
     * @param key
     * @param data
     */
    void setData(Object key, Object data);

    /**
     * 清空缓存
     *
     * @param key
     */
    void clear(Object key);
}
