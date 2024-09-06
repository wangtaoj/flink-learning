package com.wangtao.flink.quickstart;

import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.time.LocalDate;

/**
 * @author wangtao
 * Created at 2024-08-26
 */
public class Jsr310Task {

    /**
     * 由于Java17对于模块化限制做了增强，即使是一个module放在classpath上作为一个unnamed module
     * 反射访问非公共属性会报错，需要手动增加启动参数，公开访问权限
     *
     * flink对于jsr310新加的日期API，序列化时使用了反射，故而需要增加以下启动参数，公开反射的访问权限
     * 注: 如果是打包扔到flink集群上运行，则不用，因为flink发行包已经加了这些启动参数
     * <a href="https://nightlies.apache.org/flink/flink-docs-release-1.18/zh/docs/deployment/java_compatibility/">官方链接</a>
     *
     * --add-opens java.base/java.util=ALL-UNNAMED
     * --add-opens java.base/java.time=ALL-UNNAMED
     */
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<LocalDate> ds = env.fromElements(LocalDate.now(), LocalDate.now());
        ds.print();
        env.execute("First Flink Task");
    }
}
