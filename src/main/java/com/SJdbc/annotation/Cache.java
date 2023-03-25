package com.SJdbc.annotation;

import com.SJdbc.executor.cache.impl.DefaultCacheExecutor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cache {

    Class<?> exec() default DefaultCacheExecutor.class;

    long expireTime() default 30L;
}
