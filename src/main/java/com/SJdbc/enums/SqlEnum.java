package com.SJdbc.enums;

/**
 * sql枚举
 */
public enum SqlEnum {

    SELECT("SELECT"),
    INSERT("INSERT"),
    UPDATE("UPDATE"),
    DELETE("DELETE"),
    COUNT("COUNT"),
    FROM("FROM"),
    WHERE("WHERE"),
    AND("AND"),
    LIKE("LIKE"),
    IN("IN"),
    OR("OR"),
    SET("SET"),
    ORDER_BY("ORDER BY"),
    ASC("ASC"),
    DESC("DESC"),
    LIMIT("LIMIT");

    private final String word;

    SqlEnum(String word) {
        this.word = word;
    }

    public String getWord() {
        return word;
    }
}
