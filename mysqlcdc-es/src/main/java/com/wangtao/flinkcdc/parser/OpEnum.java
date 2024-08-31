package com.wangtao.flinkcdc.parser;

/**
 * @author wangtao
 * Created at 2024-08-31
 */
public enum OpEnum {

    /**
     * 直接读自表里的数据, 不是来自binlog
     */
    READ("r"),

    INSERT("c"),

    UPDATE("u"),

    DELETE("d"),
    ;

    private final String value;

    OpEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static OpEnum of(String value) {
        for (OpEnum opEnum : OpEnum.values()) {
            if (opEnum.getValue().equals(value)) {
                return opEnum;
            }
        }
        throw new IllegalArgumentException("unknown op type: " + value);
    }
}
