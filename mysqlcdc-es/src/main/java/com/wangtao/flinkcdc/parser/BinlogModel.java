package com.wangtao.flinkcdc.parser;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author wangtao
 * Created at 2024-08-31
 */
@ToString
@NoArgsConstructor
@Setter
@Getter
public class BinlogModel<T> {

    private BinlogSource source;

    private T before;

    private T after;

    /**
     * 操作类型
     */
    private String op;

    @ToString
    @NoArgsConstructor
    @Setter
    @Getter
    public static class BinlogSource {

        private String db;

        private String table;
    }
}
