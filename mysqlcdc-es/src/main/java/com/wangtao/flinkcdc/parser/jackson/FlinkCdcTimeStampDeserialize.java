package com.wangtao.flinkcdc.parser.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.io.Serial;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * 针对数据库字段类型为TIMESTAMP的解析
 * JSON字符串中的值形如2018-06-20T13:37:03Z
 * 该值是UTC时区的时间，由于TIMESTAMP类型在MySQL带有时区信息，已经正确转换了时区
 *
 * @author wangtao
 * Created at 2024-08-31
 */
public class FlinkCdcTimeStampDeserialize extends StdDeserializer<Date> {

    @Serial
    private static final long serialVersionUID = 5574546719693618532L;

    public FlinkCdcTimeStampDeserialize() {
        super(Date.class);
    }

    /**
     * JSON值为null时, 不会进入该方法
     */
    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String str = p.getValueAsString().replace("Z", "");
        // 注意是UTC时区的时间
        LocalDateTime localDateTime = LocalDateTime.parse(str);
        return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    }
}
