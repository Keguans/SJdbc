package com.SJdbc.enums;

/**
 * sql符号枚举
 */
public enum SqlCharacterEnum {

    EQUAL("="),
    LEFT_BRACKETS("("),
    RIGHT_BRACKETS(")"),
    GREAT_THEN(">"),
    LESS_THEN("<"),
    GREAT_THEN_AND_EQUAL(">="),
    LESS_THEN_AND_EQUAL("<=");

    private final String word;

    SqlCharacterEnum(String word) {
        this.word = word;
    }

    public String getWord() {
        return word;
    }
}
