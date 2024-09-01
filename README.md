### What
Flink学习笔记

### Flink CDC踩的坑
基于JsonDebeziumDeserializationSchema自带的序列化器
#### 关于Decimal类型
JSON字符串中默认是一个Base64编码的字符串

解决方式:
```java
Map<String, Object> customConverterConfigs = Map.of(
    // Decimal类型使用数字而不是base64编码
    JsonConverterConfig.DECIMAL_FORMAT_CONFIG, DecimalFormat.NUMERIC.name()
);
var deserializer = new JsonDebeziumDeserializationSchema(false, customConverterConfigs);
```
### 关于日期类型
* DATE：JSON字符串展示的是原日期到1970-01-01的天数
* DATETIME：把这个时间作为UTC时区的时间，然后返回到1970-01-01 00:00:01的毫秒数，这样对于国内东八区来说，这个毫秒数就多个8个小时。是个错误的值，因为把东八区的时间直接作为UTC的时间来计算，而不是转换成UTC的时间再计算毫秒数。
* TIMESTAMP：由于MySQL存的是1970-01-01 00:00:01的秒数，在JSON字符串中基于MySQL的`time_zone`系统变量转换成UTC时区的时间，时间是对的，相比国内东八区而言，会少8个小时，解析时需要带着时区去解析。形如2018-06-20T13:37:03Z

对于上面说的情况，转成Java对象时，都需要自己写Jackson的反序列化器来解决。

参见`FlinkCdcDateDeserialize`、`FlinkCdcDateTimeDeserialize`、`FlinkCdcTimeStampDeserialize`。

[debezium对于这部分的文档说明](https://debezium.io/documentation/reference/2.7/connectors/mysql.html#mysql-data-types)
### 关于JSR310模块在Flink中的使用
* 在JDK8中，由于没有模块化机制限制，可以正常在POJO中使用。
* 而在JDK9+版本，由于Java新增了模块化机制，导致无法在Flink中使用，Flink在序列化时会报错，只能退而求次使用java.util.Date API。
