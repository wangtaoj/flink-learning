package com.wangtao.flinkcdc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import com.wangtao.flinkcdc.parser.BinlogModel;
import com.wangtao.flinkcdc.parser.OpEnum;
import com.wangtao.flinkcdc.po.MysqlCdc;
import com.wangtao.flinkcdc.util.JsonUtils;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.connector.elasticsearch.sink.Elasticsearch7SinkBuilder;
import org.apache.flink.connector.elasticsearch.sink.RequestIndexer;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.apache.http.HttpHost;
import org.apache.kafka.connect.json.DecimalFormat;
import org.apache.kafka.connect.json.JsonConverterConfig;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.Serial;
import java.util.Map;
import java.util.Properties;

/**
 * @author wangtao
 * Created at 2024-08-31
 */
public class MysqlBinlogToEs {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 设置 3s 的 checkpoint 间隔
        env.enableCheckpointing(3000);

        // 底层debezium参数配置
        Properties debeziumProperties = new Properties();

        Map<String, Object> customConverterConfigs = Map.of(
                // Decimal类型使用数字而不是base64编码
                JsonConverterConfig.DECIMAL_FORMAT_CONFIG, DecimalFormat.NUMERIC.name()
        );

        // MySQL CDC依赖flink table api
        MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
                .hostname("127.0.0.1")
                .port(3306)
                .databaseList("test")
                .tableList("test.mysqlcdc")
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

        SingleOutputStreamOperator<BinlogModel<MysqlCdc>> binlogStream = env.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source")
                .setParallelism(4)
                .flatMap(new FlatMapFunction<>() {

                    @Serial
                    private static final long serialVersionUID = -2580566596759106029L;

                    @Override
                    public void flatMap(String value, Collector<BinlogModel<MysqlCdc>> out) {
                        BinlogModel<MysqlCdc> binlogModel = JsonUtils.jsonToObj(value, new TypeReference<>() {});
                        out.collect(binlogModel);
                    }
                });
        // 同步到es
        binlogStream.sinkTo(
                new Elasticsearch7SinkBuilder<BinlogModel<MysqlCdc>>()
                // 下面的设置使 sink 在接收每个元素之后立即提交，否则这些元素将被缓存起来
                .setBulkFlushMaxActions(1)
                .setHosts(new HttpHost("127.0.0.1", 9200, "http"))
                .setEmitter((element, context, indexer) -> syncToEs(element, indexer))
                .build()
        ).setParallelism(1);
        env.execute("MySQL To Es");
    }

    private static void syncToEs(BinlogModel<MysqlCdc> binlogModel, RequestIndexer indexer) {
        final String indexName = "mysqlcdc";
        OpEnum opEnum = OpEnum.of(binlogModel.getOp());
        MysqlCdc mysqlCdc = switch (opEnum) {
            case READ, INSERT, UPDATE -> binlogModel.getAfter();
            case DELETE -> binlogModel.getBefore();
        };
        String jsonReqBody = JsonUtils.objToJson(mysqlCdc);
        switch (opEnum) {
            case READ, INSERT, UPDATE -> {
                IndexRequest indexRequest = Requests.indexRequest()
                        .index(indexName)
                        .id(String.valueOf(mysqlCdc.getId()))
                        .source(jsonReqBody, XContentType.JSON);
                indexer.add(indexRequest);
            }
            case DELETE -> {
                DeleteRequest deleteRequest = Requests.deleteRequest(indexName)
                        .id(String.valueOf(mysqlCdc.getId()));
                indexer.add(deleteRequest);
            }
        }
    }
}
