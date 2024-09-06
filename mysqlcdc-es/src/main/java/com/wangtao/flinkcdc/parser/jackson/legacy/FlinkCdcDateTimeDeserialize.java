package com.wangtao.flinkcdc.parser.jackson.legacy;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.io.Serial;
import java.util.Date;

/**
 * 针对数据库字段类型为DATETIME的解析
 * JSON字符串中的值代表原来时间到1970-01-01 00:00:00的毫秒数
 * 由于MySQL DATETIME类型没有时区信息，是把时间当做UTC时区计算的
 * 所以解析时需要把时间转换回来
 * 源: 错把UTC+8当做UTC，导致毫秒数增加了8小时
 * 还原: 把毫秒数减去8小时
 *
 * @author wangtao
 * Created at 2024-08-31
 */
@Deprecated
public class FlinkCdcDateTimeDeserialize extends StdDeserializer<Date> {

    @Serial
    private static final long serialVersionUID = -6189047907520826779L;

    /**
     * 8小时
     */
    private static final long DIFF_MILLI = 8 * 60 * 60 * 1000;

    protected FlinkCdcDateTimeDeserialize() {
        super(Date.class);
    }

    /**
     * JSON值为null时, 不会进入该方法
     */
    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        long epochMilli = p.getLongValue();
        // 把毫秒数减去8小时
        return new Date(epochMilli - DIFF_MILLI);
    }
}
