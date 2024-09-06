### What
Flink学习笔记

### 版本
Flink 1.18.1、MySQL CDC 3.0.1

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
#### 关于日期类型
* DATE：JSON字符串展示的是原日期到1970-01-01的天数
* DATETIME：把这个时间作为UTC时区的时间，然后返回的是纪元毫秒数，这样对于国内东八区来说，这个毫秒数就多个8个小时。是个错误的值，因为把东八区的时间直接作为UTC的时间来计算，而不是转换成UTC的时间再计算毫秒数。
* TIMESTAMP：由于MySQL存的是纪元秒数，在JSON字符串中基于MySQL的`time_zone`系统变量转换成UTC时区的时间，时间是对的，相比国内东八区而言，会少8个小时，解析时需要带着时区去解析。形如2018-06-20T13:37:03Z

对于上面说的情况，转成Java对象时，都需要自己写Jackson的反序列化器来解决。

参见自定义的(非内置)`FlinkCdcDateDeserialize`、`FlinkCdcDateTimeDeserialize`、`FlinkCdcTimeStampDeserialize`。

#### 纪元毫秒数（millisecond since epoch
通常指的是自 Unix 纪元时间（Epoch Time）以来经过的毫秒数。Unix 纪元时间是指 1970 年 1 月 1 日 00:00:00 UTC（协调世界时）。

举例说明：
* 0 毫秒：表示 1970 年 1 月 1 日 00:00:00 UTC。
* 1000 毫秒：表示 1970 年 1 月 1 日 00:00:01 UTC（1970 年 1 月 1 日 0 时 0 分 1 秒）。
* 86400000 毫秒：表示 1970 年 1 月 2 日 00:00:00 UTC（因为 1 天有 86,400,000 毫秒）。
* 一个纪元毫秒数+一个时区，便可以确定是这个时区的具体哪个时间。UTC时区-》指定时区

[debezium对于这部分的文档说明](https://debezium.io/documentation/reference/2.7/connectors/mysql.html#mysql-data-types)
### 关于JSR310模块在Flink中的使用
* 在JDK8中，由于没有模块化机制限制，可以正常在POJO中使用。
* 在JDK11中，虽然有模块化限制，但是对于unamed module而言，是可以通过反射访问模块中的私有变量的，只不过会有一个警告。
* 而在JDK17+版本，由于Java增强了模块化限制，即使是一个module放在classpath上作为一个unnamed module时，通过反射访问非公共属性时还是会报错，需要手动增加启动参数，公开反射访问权限。否则无法在Flink中使用，Flink在序列化时会报错。

解决方式：
增加以下jvm启动参数即可
```text
# 公开java.base模块的java.util、java.time包的反射访问权限给所有的未命名模块
--add-opens java.base/java.util=ALL-UNNAMED
--add-opens java.base/java.time=ALL-UNNAMED
```
**注：如果是在flink集群中运行任务，则无需添加，因为flink发行包中已经加了这些启动参数。**

[Flink关于Java兼容性文档说明](https://nightlies.apache.org/flink/flink-docs-release-1.18/zh/docs/deployment/java_compatibility/)
