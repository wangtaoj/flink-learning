package com.wangtao.flinkcdc;

import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Properties;

/**
 * @author wangtao
 * Created at 2024-09-09
 */
public class MysqlBinlogUseMyDebeziumDeserializ {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 设置 3s 的 checkpoint 间隔
        env.enableCheckpointing(3000);

        // 底层debezium参数配置
        Properties debeziumProperties = new Properties();

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
                .deserializer(new FriendlyDebeziumDeserializationSchema())
                .build();
        env.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source")
                .setParallelism(4)
                .print()
                .setParallelism(1); // 为了保证顺序，消费节点设置并行度为1
        env.execute("MySQL Binlog");
    }
}
