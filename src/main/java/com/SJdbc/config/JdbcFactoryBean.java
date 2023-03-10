package com.SJdbc.config;

import com.SJdbc.proxy.JdbcProxy;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.Proxy;

/**
 * 用于将 @JdbcMapperScan 注解中指定的包下的类进行动态代理，后注册到 IOC 容器
 *
 * @param <T>
 */
public class JdbcFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> aClass;

    public JdbcFactoryBean(Class<T> aClass) {
        this.aClass = aClass;
    }

    @Override
    @SuppressWarnings(value = "all")
    public T getObject() throws Exception {
        return (T) Proxy.newProxyInstance(
                aClass.getClassLoader(),
                new Class[]{aClass},
                new JdbcProxy()
        );
    }

    @Override
    public Class<?> getObjectType() {
        return aClass;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
