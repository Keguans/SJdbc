package com.SJdbc.model;

import com.SJdbc.annotation.Column;
import com.SJdbc.annotation.Key;
import com.SJdbc.annotation.Table;
import com.SJdbc.enums.SqlCharacterEnum;
import com.SJdbc.enums.SqlEnum;
import com.SJdbc.function.JdbcFunction;
import com.SJdbc.util.ColumnUtil;
import com.SJdbc.util.JdbcUtil;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * sql 拼装类
 */
public class QueryModel {

    /**
     * sql 语句
     */
    public final StringBuilder sql = new StringBuilder();

    private Class<?> aClass;

    public QueryModel(Class<?> aClass) {
        this.aClass = aClass;
    }

    /**
     * select
     *
     * @return
     */
    public QueryModel select() {
        sql.append(SqlEnum.SELECT).append(" ");
        return this;
    }

    /**
     * update
     *
     * @return
     */
    public QueryModel update() {
        sql.append(SqlEnum.UPDATE).append(" ");
        return this;
    }

    /**
     * update
     *
     * @return
     */
    public QueryModel delete() {
        sql.append(SqlEnum.DELETE).append(" ");
        return this;
    }

    public QueryModel tableName() {
        Table table = aClass.getAnnotation(Table.class);
        sql.append(Objects.isNull(table) || !StringUtils.hasText(table.name()) ? aClass.getName() : table.name()).append(" ");
        return this;
    }

    /**
     * 数量
     *
     * @return
     */
    public QueryModel count() {
        sql.append(SqlEnum.COUNT).append("(1) ");
        return this;
    }

    /**
     * 数量
     *
     * @param sf
     * @return
     * @param <T>
     * @param <R>
     */
    public <T, R> QueryModel count(JdbcFunction<T, R> sf) {
        String fieldName = ColumnUtil.getColumn(sf);
        sql.append(SqlEnum.COUNT).append("(").append(fieldName).append(") ");
        return this;
    }

    /**
     * table
     *
     * @return
     */
    public QueryModel table(String... column) {
        Field[] declaredFields = aClass.getDeclaredFields();
        List<String> columnList = Arrays.asList(column);
        for (Field field : declaredFields) {
            String fieldName;
            Column annotation = field.getAnnotation(Column.class);
            if (Objects.nonNull(annotation)) {
                fieldName = StringUtils.hasText(annotation.filed()) ? annotation.filed() : field.getName();
            } else {
                Key key = field.getAnnotation(Key.class);
                if (Objects.nonNull(key)) {
                    fieldName = StringUtils.hasText(key.column()) ? key.column() : field.getName();
                } else {
                    fieldName = field.getName();
                }
            }
            // 指定查询字段
            if (columnList.isEmpty()) {
                sql.append(fieldName).append(fieldName.equals(field.getName()) ? "" : " AS " + field.getName()).append(", ");
            }
            if (columnList.contains(fieldName)) {
                sql.append(fieldName).append(fieldName.equals(field.getName()) ? "" : " AS " + field.getName()).append(", ");
            }
        }
        if (sql.toString().endsWith(", ")) {
            sql.deleteCharAt(sql.length() - 1);
            sql.deleteCharAt(sql.length() - 1);
        }
        return this;
    }

    /**
     * from
     *
     * @return
     */
    public QueryModel from() {
        Table table = aClass.getAnnotation(Table.class);
        sql.append(" ")
                .append(SqlEnum.FROM)
                .append(" ")
                .append(Objects.isNull(table) || !StringUtils.hasText(table.name()) ? aClass.getName() : table.name());
        return this;
    }

    /**
     * where
     *
     * @return
     */
    public QueryModel where() {
        sql.append(" ").append(SqlEnum.WHERE).append(" ");
        return this;
    }

    /**
     * and
     *
     * @return
     */
    public QueryModel and() {
        sql.append(" ").append(SqlEnum.AND).append(" ");
        return this;
    }

    /**
     * 等于
     *
     * @param sf
     * @param val
     * @param <R>
     * @return
     */
    public <T, R> QueryModel eq(JdbcFunction<T, R> sf, Object val) {
        String fieldName = ColumnUtil.getColumn(sf);
        sql.append(fieldName)
                .append(" ")
                .append(SqlCharacterEnum.EQUAL.getWord())
                .append(" ")
                .append(JdbcUtil.convert(val));
        return this;
    }

    /**
     * like
     *
     * @param sf
     * @param val
     * @param <T>
     * @param <R>
     * @return
     */
    public <T, R> QueryModel like(JdbcFunction<T, R> sf, Object val) {
        String fieldName = ColumnUtil.getColumn(sf);
        sql.append(fieldName)
                .append(" ")
                .append(SqlEnum.LIKE)
                .append(" ")
                .append("\"%").append(val).append("%\"");
        return this;
    }

    /**
     * 左 like
     *
     * @param sf
     * @param val
     * @param <T>
     * @param <R>
     * @return
     */
    public <T, R> QueryModel lLike(JdbcFunction<T, R> sf, Object val) {
        String fieldName = ColumnUtil.getColumn(sf);
        sql.append(fieldName)
                .append(" ")
                .append(SqlEnum.LIKE)
                .append(" ").append("\"%").append(val).append("\"");
        return this;
    }

    /**
     * 右 like
     *
     * @param sf
     * @param val
     * @param <T>
     * @param <R>
     * @return
     */
    public <T, R> QueryModel rLike(JdbcFunction<T, R> sf, Object val) {
        String fieldName = ColumnUtil.getColumn(sf);
        sql.append(fieldName)
                .append(" ")
                .append(SqlEnum.LIKE)
                .append(" ").append("\"").append(val).append("%\"");
        return this;
    }

    /**
     * in
     *
     * @param sf
     * @param collection
     * @param <T>
     * @param <R>
     * @return
     */
    public <T, R> QueryModel in(JdbcFunction<T, R> sf, Collection<?> collection) {
        if (collection.isEmpty()) {
            return this;
        }
        String fieldName = ColumnUtil.getColumn(sf);
        sql.append(fieldName).append(" ").append(SqlEnum.IN).append(" (");
        collection.forEach(obj -> sql.append(JdbcUtil.convert(obj)).append(", "));
        sql.deleteCharAt(sql.length() - 1)
                .deleteCharAt(sql.length() - 1)
                .append(")");
        return this;
    }

    /**
     * in
     *
     * @param sf
     * @param obj
     * @param <T>
     * @param <R>
     * @return
     */
    public <T, R> QueryModel in(JdbcFunction<T, R> sf, Object... obj) {
        if (obj.length == 0) {
            return this;
        }
        String fieldName = ColumnUtil.getColumn(sf);
        sql.append(fieldName).append(" ").append(SqlEnum.IN).append(" (");
        for (Object o : obj) {
            sql.append(JdbcUtil.convert(o)).append(", ");
        }
        sql.deleteCharAt(sql.length() - 1)
                .deleteCharAt(sql.length() - 1)
                .append(")");
        return this;
    }

    /**
     * or
     *
     * @return
     */
    public QueryModel or() {
        sql.append(" ").append(SqlEnum.OR).append(" ");
        return this;
    }

    /**
     * 大于
     *
     * @param sf
     * @param val
     * @param <T>
     * @param <R>
     * @return
     */
    public <T, R> QueryModel gt(JdbcFunction<T, R> sf, Object val) {
        String column = ColumnUtil.getColumn(sf);
        sql.append(column).append(" ").append(SqlCharacterEnum.GREAT_THEN.getWord()).append(" ")
                .append(JdbcUtil.convert(val));
        return this;
    }

    /**
     * 大于等于
     *
     * @param sf
     * @param val
     * @param <T>
     * @param <R>
     * @return
     */
    public <T, R> QueryModel ge(JdbcFunction<T, R> sf, Object val) {
        String column = ColumnUtil.getColumn(sf);
        sql.append(column).append(" ").append(SqlCharacterEnum.GREAT_THEN_AND_EQUAL.getWord()).append(" ")
                .append(JdbcUtil.convert(val));
        return this;
    }

    /**
     * 小于
     *
     * @param sf
     * @param val
     * @param <T>
     * @param <R>
     * @return
     */
    public <T, R> QueryModel lt(JdbcFunction<T, R> sf, Object val) {
        String column = ColumnUtil.getColumn(sf);
        sql.append(column).append(" ").append(SqlCharacterEnum.LESS_THEN.getWord()).append(" ")
                .append(JdbcUtil.convert(val));
        return this;
    }

    /**
     * 小于等于
     *
     * @param sf
     * @param val
     * @param <T>
     * @param <R>
     * @return
     */
    public <T, R> QueryModel le(JdbcFunction<T, R> sf, Object val) {
        String column = ColumnUtil.getColumn(sf);
        sql.append(column).append(" ").append(SqlCharacterEnum.LESS_THEN_AND_EQUAL.getWord()).append(" ")
                .append(JdbcUtil.convert(val));
        return this;
    }

    /**
     * set
     *
     * @param sf
     * @param val
     * @return
     * @param <T>
     * @param <R>
     */
    public <T, R> QueryModel set(JdbcFunction<T, R> sf, Object val) {
        String column = ColumnUtil.getColumn(sf);
        sql.append(SqlEnum.SET)
                .append(" ")
                .append(column)
                .append(" ")
                .append(SqlCharacterEnum.EQUAL.getWord())
                .append(" ")
                .append(JdbcUtil.convert(val));
        return this;
    }

    /**
     * order by asc
     *
     * @param sf
     * @param <T>
     * @param <R>
     * @return
     */
    public <T, R> QueryModel orderBy(JdbcFunction<T, R> sf) {
        String column = ColumnUtil.getColumn(sf);
        sql.append(" ").append(SqlEnum.ORDER_BY).append(" ").append(column).append(" ").append(SqlEnum.ASC);
        return this;
    }

    /**
     * order by desc
     *
     * @param sf
     * @param <T>
     * @param <R>
     * @return
     */
    public <T, R> QueryModel orderByDesc(JdbcFunction<T, R> sf) {
        String column = ColumnUtil.getColumn(sf);
        sql.append(" ").append(SqlEnum.ORDER_BY).append(" ").append(column).append(" ").append(SqlEnum.DESC);
        return this;
    }

    /**
     * limit
     *
     * @param a
     * @param b
     * @return
     */
    public QueryModel limit(int a, int... b) {
        sql.append(" ").append(SqlEnum.LIMIT).append(" ").append(a);
        if (b.length > 0) {
            sql.append(", ").append(b[0]);
        }
        return this;
    }

    /**
     * 符号
     *
     * @return
     */
    public QueryModel character(SqlCharacterEnum character) {
        sql.append(character.getWord());
        return this;
    }

    /**
     * 清空 sql 语句
     */
    public void clearSql() {
        sql.setLength(0);
    }

    /**
     * 清空所有
     */
    public void clearQueryModel() {
        sql.setLength(0);
        this.aClass = null;
    }
}
