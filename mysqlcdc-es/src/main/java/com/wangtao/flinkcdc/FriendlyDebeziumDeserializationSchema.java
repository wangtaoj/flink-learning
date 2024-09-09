package com.wangtao.flinkcdc;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ververica.cdc.debezium.DebeziumDeserializationSchema;
import com.wangtao.flinkcdc.util.DateUtils;
import io.debezium.time.Date;
import io.debezium.time.Timestamp;
import io.debezium.time.ZonedTimestamp;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.util.Collector;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.source.SourceRecord;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 为JsonDebeziumDeserializationSchema的简化版
 * 修改了以下两点
 * DECIAML默认为数字形式
 * DATE、DATETIME、TIMESTAMP以字符串形式展示
 *
 * @author wangtao
 * Created at 2024-09-09
 */
public class FriendlyDebeziumDeserializationSchema implements DebeziumDeserializationSchema<String> {

    @Serial
    private static final long serialVersionUID = 6087204632497702925L;

    private static final JsonNodeFactory JSON_NODE_FACTORY = JsonNodeFactory.withExactBigDecimals(false);

    private final ObjectMapper objectMapper;

    private final Map<String, Converter> localConvertMap = new HashMap<>();

    public FriendlyDebeziumDeserializationSchema() {
        this.objectMapper = createDefaultObjectMapper();
        this.initDefaultConverts();
    }

    public FriendlyDebeziumDeserializationSchema(ObjectMapper objectMapper, Map<String, Converter> customerConvertMap) {
        Objects.requireNonNull(objectMapper);
        this.objectMapper = objectMapper;
        this.initDefaultConverts();
        if (Objects.nonNull(customerConvertMap)) {
            localConvertMap.putAll(customerConvertMap);
        }
    }

    private ObjectMapper createDefaultObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        return objectMapper;
    }

    private void initDefaultConverts() {
        // DECIMAL 或者 BIGINT UNSIGNED
        localConvertMap.put(Decimal.LOGICAL_NAME, ((schema, value) -> {
            if (value instanceof BigDecimal numberValue) {
                return JSON_NODE_FACTORY.numberNode(numberValue);
            }
            throw new DataException("Invalid type " + value.getClass());
        }));

        // DATE
        localConvertMap.put(Date.SCHEMA_NAME, ((schema, value) -> {
            if (value instanceof Integer epochDay) {
                LocalDate localDate = LocalDate.ofEpochDay(epochDay);
                return JSON_NODE_FACTORY.textNode(DateUtils.formatDate(localDate));
            }
            throw new DataException("Invalid type " + value.getClass());
        }));

        // DATETIME(无时区), 值为UTC时间对应的毫秒数
        localConvertMap.put(Timestamp.SCHEMA_NAME, ((schema, value) -> {
            if (value instanceof Long epochMilli) {
                LocalDateTime localDateTime = Instant.ofEpochMilli(epochMilli).atOffset(ZoneOffset.UTC).toLocalDateTime();
                return JSON_NODE_FACTORY.textNode(DateUtils.formatDateTime(localDateTime));
            }
            throw new DataException("Invalid type " + value.getClass());
        }));

        // TIMESTAMP, 值形如2024-09-06T18:54:03Z, Z代表UTC时区
        localConvertMap.put(ZonedTimestamp.SCHEMA_NAME, ((schema, value) -> {
            if (value instanceof String str) {
                Instant instant = Instant.parse(str);
                return JSON_NODE_FACTORY.textNode(DateUtils.formatInstant(instant));
            }
            throw new DataException("Invalid type " + value.getClass());
        }));
    }

    @Override
    public void deserialize(SourceRecord record, Collector<String> out) throws Exception {
        JsonNode jsonNode = convertToJson(record.valueSchema(), record.value());
        out.collect(objectMapper.writeValueAsString(jsonNode));
    }

    private JsonNode convertToJson(Schema schema, Object value) {
        if (Objects.isNull(value)) {
            return JSON_NODE_FACTORY.nullNode();
        }
        if (Objects.nonNull(schema.name())) {
            Converter converter = localConvertMap.get(schema.name());
            if (Objects.nonNull(converter)) {
                return converter.convert(schema, value);
            }
        }
        try {
            final Schema.Type schemaType = schema.type();
            switch (schemaType) {
                case INT8:
                    return JSON_NODE_FACTORY.numberNode((Byte) value);
                case INT16:
                    return JSON_NODE_FACTORY.numberNode((Short) value);
                case INT32:
                    return JSON_NODE_FACTORY.numberNode((Integer) value);
                case INT64:
                    return JSON_NODE_FACTORY.numberNode((Long) value);
                case FLOAT32:
                    return JSON_NODE_FACTORY.numberNode((Float) value);
                case FLOAT64:
                    return JSON_NODE_FACTORY.numberNode((Double) value);
                case BOOLEAN:
                    return JSON_NODE_FACTORY.booleanNode((Boolean) value);
                case STRING:
                    CharSequence charSeq = (CharSequence) value;
                    return JSON_NODE_FACTORY.textNode(charSeq.toString());
                case BYTES:
                    if (value instanceof byte[])
                        return JSON_NODE_FACTORY.binaryNode((byte[]) value);
                    else if (value instanceof ByteBuffer)
                        return JSON_NODE_FACTORY.binaryNode(((ByteBuffer) value).array());
                    else
                        throw new DataException("Invalid type for bytes type: " + value.getClass());
                case ARRAY: {
                    Collection<?> collection = (Collection<?>) value;
                    ArrayNode list = JSON_NODE_FACTORY.arrayNode();
                    Schema elementSchema = schema.valueSchema();
                    for (Object elem : collection) {
                        JsonNode fieldValue = convertToJson(elementSchema, elem);
                        list.add(fieldValue);
                    }
                    return list;
                }
                case STRUCT: {
                    Struct struct = (Struct) value;
                    if (!struct.schema().equals(schema))
                        throw new DataException("Mismatching schema.");
                    ObjectNode obj = JSON_NODE_FACTORY.objectNode();
                    for (Field field : schema.fields()) {
                        obj.set(field.name(), convertToJson(field.schema(), struct.get(field)));
                    }
                    return obj;
                }
            }
            throw new DataException("Couldn't convert " + value + " to JSON.");
        } catch (ClassCastException e) {
            String schemaTypeStr = schema.type().toString();
            throw new DataException("Invalid type for " + schemaTypeStr + ": " + value.getClass());
        }
    }

    @Override
    public TypeInformation<String> getProducedType() {
        return BasicTypeInfo.STRING_TYPE_INFO;
    }

    public interface Converter extends Serializable {

        JsonNode convert(Schema schema, Object value);
    }
}
