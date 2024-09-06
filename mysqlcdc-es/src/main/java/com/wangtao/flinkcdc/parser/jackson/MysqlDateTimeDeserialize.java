package com.wangtao.flinkcdc.parser.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.io.Serial;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * MySQL CDC对于DATETIME类型
 * 由于DATETIME类型没有时区, MySQL CDC直接把时间当做UTC时区转换成了纪元毫秒数
 * 比如2024-09-06 13:54:03这个时间
 * 2024-09-06 13:54:03结合UTC时区转成的毫秒数会多8个小时
 *
 * 因为2024-09-06 13:54:03 UTC = 2024-09-06 21:54:03 UTC+8
 * 所以如果用这个毫秒数+东八区时区转时间的会多8个小时
 *
 * 这里可以取巧，转成UTC的时间，再去掉UTC时区获取前面的时间部分即可。
 *
 * @author wangtao
 * Created at 2024-09-06
 */
public class MysqlDateTimeDeserialize extends StdDeserializer<LocalDateTime> {

    @Serial
    private static final long serialVersionUID = 5778485322993107284L;

    protected MysqlDateTimeDeserialize() {
        super(Instant.class);
    }

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        long epochMilli = p.getLongValue();
        return Instant.ofEpochMilli(epochMilli).atOffset(ZoneOffset.UTC).toLocalDateTime();
    }
}
