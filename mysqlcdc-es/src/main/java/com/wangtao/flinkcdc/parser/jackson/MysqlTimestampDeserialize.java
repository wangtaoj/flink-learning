package com.wangtao.flinkcdc.parser.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.io.Serial;
import java.time.Instant;

/**
 * MySQL CDC对于TIMESTAMP类型
 * 形如: 2024-09-06T18:54:03Z, Z代表UTC时区
 * 已经根据MySQL的time_zone系统变量正确转换成UTC时区的字符串格式
 * 这个格式就是Instant toString方法结果格式，可以直接通过Instant.parse方法解析
 *
 * @author wangtao
 * Created at 2024-09-06
 */
public class MysqlTimestampDeserialize extends StdDeserializer<Instant> {

    @Serial
    private static final long serialVersionUID = 5778485322993107284L;

    protected MysqlTimestampDeserialize() {
        super(Instant.class);
    }

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return Instant.parse(p.getText());
    }
}
