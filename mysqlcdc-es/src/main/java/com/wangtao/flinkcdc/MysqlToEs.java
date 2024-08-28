package com.wangtao.flinkcdc;

import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @author wangtao
 * Created at 2024-08-27
 */
public class MysqlToEs {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 设置 3s 的 checkpoint 间隔
        env.enableCheckpointing(3000);

        // MySQL CDC依赖flink table api
        MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
                .hostname("127.0.0.1")
                .port(3306)
                .databaseList("test")
                .tableList("test.t2") // databasename.tablename
                .username("root")
                .password("123456")
                .deserializer(new JsonDebeziumDeserializationSchema()) // 将 SourceRecord 转换为 JSON 字符串
                .startupOptions(StartupOptions.latest()) // 从最新的binlog位置监听, 不会读取历史数据
                .serverId("5401-5404") // 模拟slave serverId, 读取阶段设置并行度为4, 范围配置需要>=4
                .build();
        env.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source")
                .setParallelism(4)
                .print()
                .setParallelism(1); // 为了保证顺序，消费节点设置并行度为1
        env.execute("MysqlToEs");
    }
}
