package com.SJdbc.function;

import java.io.Serializable;
import java.util.function.Function;

/**
 * 具有序列化能力的 function
 *
 * @param <T>
 * @param <R>
 */
@FunctionalInterface
public interface JdbcFunction<T, R> extends Function<T, R>, Serializable {
}
