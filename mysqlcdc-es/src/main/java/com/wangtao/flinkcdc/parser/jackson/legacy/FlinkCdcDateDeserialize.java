package com.wangtao.flinkcdc.parser.jackson.legacy;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.io.Serial;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * 针对数据库字段类型为DATE的解析
 * JSON字符串中的值代表原始日期到1970-01-01的天数
 * @author wangtao
 * Created at 2024-08-31
 */
@Deprecated
public class FlinkCdcDateDeserialize extends StdDeserializer<Date> {

    @Serial
    private static final long serialVersionUID = 4966438857113124539L;

    public FlinkCdcDateDeserialize() {
        super(Date.class);
    }

    /**
     * JSON值为null时, 不会进入该方法
     */
    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        long epochDay = p.getLongValue();
        LocalDate localDate = LocalDate.ofEpochDay(epochDay);
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }
}
