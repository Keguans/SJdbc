package com.SJdbc.annotation;

import com.SJdbc.enums.IdTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 主键
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Key {

    /**
     * 字段名
     *
     * @return
     */
    String column() default "id";

    /**
     * 主键类型
     *
     * @return
     */
    IdTypeEnum type() default IdTypeEnum.ID_AUTO;
}