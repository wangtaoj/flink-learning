package com.wangtao.flinkcdc;

import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.connect.json.DecimalFormat;
import org.apache.kafka.connect.json.JsonConverterConfig;

import java.util.Map;
import java.util.Properties;

/**
 * @author wangtao
 * Created at 2024-08-27
 */
public class MysqlBinlog {

    /**
     * startupOptions
     * StartupOptions.initial(): 默认值, 全量(表中已有数据)加增量binlog
     * 全量读取阶段没有锁，可以并行读取，增量监听binlog时并行度会自动变成单一并行度，以保证binlog顺序性
     * StartupOptions.latest(): 从最新的binlog位置监听, 不会读取表里现有数据
     * StartupOptions.earliest(): 从binlog最早的位置开始读取, 不会读取表里现有数据，
     * 若binlog被清理，意味着读取的数据就会不全
     *
     * serverId
     * 由于全量读取阶段时，是无锁模式，单一分片读取最后一条数据和读取此时的binlog位置不是原子性操作，所以需要把这个空隙的
     * 变化应用进来，这个动作是读取binlog的，所以有多少个并行度就需要有多少个serverId
     * 值为一个整数或者整数范围，不设置默认会从5400和6400之间生成一个随机数
     *
     * 返回的JSON值
     * op: SQL操作
     * r: 不是来自binlog
     * c: insert
     * u: update
     * d: delete
     */
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 设置 3s 的 checkpoint 间隔
        env.enableCheckpointing(3000);

        // 底层debezium参数配置
        Properties debeziumProperties = new Properties();
        //debeziumProperties.put("decimal.handling.mode", "string");

        Map<String, Object> customConverterConfigs = Map.of(
                // Decimal类型使用数字而不是base64编码
                JsonConverterConfig.DECIMAL_FORMAT_CONFIG, DecimalFormat.NUMERIC.name()
        );

        // MySQL CDC依赖flink table api
        MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
                .hostname("127.0.0.1")
                .port(3306)
                .databaseList("test")
                .tableList("test.mysqlcdc") // databasename.tablename
                .username("root")
                .password("123456")
                .serverTimeZone("Asia/Shanghai")
                // 模拟slave serverId, 读取阶段设置并行度为4, 范围配置需要>=4
                .serverId("5401-5404")
                // 读取表中全量数据, 并监听增量数据
                .startupOptions(StartupOptions.initial())
                .debeziumProperties(debeziumProperties)
                // 将 SourceRecord 转换为 JSON 字符串
                .deserializer(new JsonDebeziumDeserializationSchema(false, customConverterConfigs))
                .build();
        env.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source")
                .setParallelism(4)
                .print()
                .setParallelism(1); // 为了保证顺序，消费节点设置并行度为1
        env.execute("MySQL Binlog");
    }
}
