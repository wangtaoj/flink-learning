package com.wangtao.flinkcdc.parser.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.io.Serial;
import java.time.LocalDate;

/**
 * MySQL CDC对于DATE类型
 * 返回的值代表原始日期到1970-01-01的天数
 *
 * @author wangtao
 * Created at 2024-08-31
 */
public class MysqlDateDeserialize extends StdDeserializer<LocalDate> {

    @Serial
    private static final long serialVersionUID = 4966438857113124539L;

    public MysqlDateDeserialize() {
        super(LocalDate.class);
    }

    /**
     * JSON值为null时, 不会进入该方法
     */
    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        long epochDay = p.getLongValue();
        return LocalDate.ofEpochDay(epochDay);
    }
}
