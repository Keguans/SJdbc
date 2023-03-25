package com.SJdbc.executor.cache.impl;

import com.SJdbc.executor.cache.CacheExecutor;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认缓存执行器
 */
public class DefaultCacheExecutor implements CacheExecutor {

    private final Long expireTime;

    public DefaultCacheExecutor(Long expireTime) {
        this.expireTime = expireTime;
    }

    private ConcurrentHashMap<String, Data> cache;

    @Override
    public Object getData(Object key) {
        init();
        Data data = cache.get(String.valueOf(key));
        if (Objects.isNull(data)) {
            return null;
        }
        // 过期移除缓存
        if (data.getLocalDateTime().isBefore(LocalDateTime.now())) {
            cache.remove(String.valueOf(key));
            return null;
        }
        return data.getData();
    }

    @Override
    public void setData(Object key, Object data) {
        init();
        if (Objects.nonNull(key) && Objects.nonNull(data)) {
            cache.put(String.valueOf(key), new Data(data, LocalDateTime.now().plusSeconds(expireTime)));
        }
    }

    /**
     * 清空缓存
     */
    @Override
    public void clear(Object key) {
        init();
        cache.remove(String.valueOf(key));
    }

    /**
     * 初始化
     */
    public void init() {
        if (Objects.nonNull(cache)) {
            return;
        }
        synchronized (this) {
            if (Objects.isNull(cache)) {
                cache = new ConcurrentHashMap<>();
            }
        }
    }

    public static class Data {

        private final Object data;

        private final LocalDateTime localDateTime;

        public Data(Object data, LocalDateTime localDateTime) {
            this.data = data;
            this.localDateTime = localDateTime;
        }

        public LocalDateTime getLocalDateTime() {
            return this.localDateTime;
        }

        public Object getData() {
            return this.data;
        }
    }
}
