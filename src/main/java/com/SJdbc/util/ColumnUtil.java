package com.SJdbc.util;

import com.SJdbc.annotation.Column;
import com.SJdbc.annotation.Table;
import com.SJdbc.function.JdbcFunction;
import org.springframework.util.StringUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 获取字段名
 */
public class ColumnUtil {

    /**
     * 字段名注解，声明表字段
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TableField {
        String value() default "";
    }

    /**
     * 默认配置
     */
    private final static String defaultSplit = "";

    private final static Integer defaultToType = 0;

    /**
     * 获取字段名称
     *
     * @param fn
     * @param <T>
     * @return
     */
    public static <T> String getFieldName(JdbcFunction<T, ?> fn) {
        return getFieldName(fn, defaultSplit, defaultToType);
    }

    /**
     * 获取字段名
     *
     * @param fn
     * @param split
     * @param toType
     * @param <T>
     * @return
     */
    public static <T> String getFieldName(JdbcFunction<T, ?> fn, String split, Integer toType) {
        Field field = getField(fn);

        // 从field取出字段名，可以根据实际情况调整
        TableField tableField = field.getAnnotation(TableField.class);
        if (tableField != null && tableField.value().length() > 0) {
            return tableField.value();
        } else {
            // 0.不做转换 1.大写 2.小写
            String fieldName = field.getName();
            if (toType == 1) {
                fieldName = fieldName.replaceAll("[A-Z]", split + "$0").toUpperCase();
            } else if (toType == 2) {
                fieldName = fieldName.replaceAll("[A-Z]", split + "$0").toLowerCase();
            } else {
                fieldName = fieldName.replaceAll("[A-Z]", split + "$0");
            }
            return fieldName;
        }
    }

    /**
     * 获取注解里的数据库字段
     *
     * @param fn
     * @param <T>
     * @return
     */
    public static <T> String getColumn(JdbcFunction<T, ?> fn) {
        Field field = getField(fn);
        // 获取注解
        Column annotation = field.getAnnotation(Column.class);
        if (Objects.nonNull(annotation) && StringUtils.hasText(annotation.filed())) {
            return annotation.filed();
        } else {
            return field.getName();
        }
    }

    /**
     * 获取注解里的数据库字段
     *
     * @param fn
     * @param <T>
     * @return
     */
    public static <T> String getColumnWithTableName(JdbcFunction<T, ?> fn) {
        // 表名
        String tableName = getTableName(fn);
        // 字段名
        String column = getColumn(fn);
        return tableName + "." + column;
    }

    /**
     * 获取 tableName
     *
     * @param fn
     * @return
     * @param <T>
     */
    private static <T> String getTableName(JdbcFunction<T, ?> fn) {
        SerializedLambda serializedLambda = getSerializedLambda(fn);
        String implClass = serializedLambda.getImplClass();
        try {
            Class<?> aClass = Class.forName(implClass.replace("/", "."));
            Table annotation = aClass.getAnnotation(Table.class);
            if (Objects.isNull(annotation)) {
                return aClass.getName();
            }
            if (!StringUtils.hasText(annotation.name())) {
                return aClass.getName();
            }
            return annotation.name();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取 字段名
     *
     * @param fn
     * @return
     */
    private static <T> Field getField(JdbcFunction<T, ?> fn) {
        SerializedLambda serializedLambda = getSerializedLambda(fn);

        // 从lambda信息取出method、field、class等
        String fieldName = serializedLambda.getImplMethodName().substring("get".length());
        fieldName = fieldName.replaceFirst(fieldName.charAt(0) + "", (fieldName.charAt(0) + "").toLowerCase());
        Field field;
        try {
            field = Class.forName(serializedLambda.getImplClass().replace("/", ".")).getDeclaredField(fieldName);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        return field;
    }

    private static <T> SerializedLambda getSerializedLambda(JdbcFunction<T, ?> fn) {
        // 从 function 取出序列化方法
        Method writeReplaceMethod;
        try {
            writeReplaceMethod = fn.getClass().getDeclaredMethod("writeReplace");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        // 从序列化方法取出序列化的lambda信息
        writeReplaceMethod.setAccessible(true);
        SerializedLambda serializedLambda;
        try {
            serializedLambda = (SerializedLambda) writeReplaceMethod.invoke(fn);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return serializedLambda;
    }
}
