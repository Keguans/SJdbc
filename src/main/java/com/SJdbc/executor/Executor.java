package com.SJdbc.executor;

/**
 * 执行器
 */
public interface Executor {

    /**
     * 获取数据
     *
     * @param key
     * @return
     */
    Object getData(Object key);
}
