package com.wangtao.flink.quickstart;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

import java.io.Serial;

/**
 * @author wangtao
 * Created at 2024-08-26
 */
public class FirstFlinkTask {

    public static void main(String[] args) throws Exception {
        // 创建执行环境, 内部会自己判断是本地环境还是flink集群环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 数据源
        DataStreamSource<String> ds = env.fromElements("aa", "bb", "cc");

        // 转换操作, flatMap输入元素映射到0个或多个其他类型元素
        SingleOutputStreamOperator<String> finalDs = ds.flatMap(new FlatMapFunction<>() {
            @Serial
            private static final long serialVersionUID = 4936362867192282272L;

            @Override
            public void flatMap(String value, Collector<String> out) {
                out.collect(value.toUpperCase());
            }
        });

        // sink操作(输出)
        finalDs.print();

        // 执行任务
        env.execute("First Flink Task");
    }
}
