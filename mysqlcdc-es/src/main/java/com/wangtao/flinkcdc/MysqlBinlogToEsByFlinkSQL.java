package com.wangtao.flinkcdc;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * @author wangtao
 * Created at 2024-08-29
 */
public class MysqlBinlogToEsByFlinkSQL {

    public static void main(String[] args) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 设置 3s 的 checkpoint 间隔
        env.enableCheckpointing(3000);

        TableEnvironment tableEnv = StreamTableEnvironment.create(env);

        String createTableSql = """
            create table mysqlcdc (
              id bigint,
              name string,
              age int,
              money decimal(20,2),
              birthday date,
              create_time timestamp(0),
              update_time timestamp(0),
              primary key(id) not enforced
            ) with (
              'connector' = 'mysql-cdc',
              'hostname' = '127.0.0.1',
              'port' = '3306',
              'username' = 'root',
              'password' = '123456',
              'database-name' = 'test',
              'table-name' = 'mysqlcdc'
            );
            """.stripIndent();
        // 建表
        tableEnv.executeSql(createTableSql);

        String sinkCreateTable = """
            create table mysqlcdcEs (
              id bigint,
              name string,
              age int,
              money decimal(20,2),
              birthday date,
              createTime timestamp(0),
              updateTime timestamp(0),
              primary key(id) not enforced
            ) with (
              'connector' = 'elasticsearch-7',
              'hosts' = 'http://127.0.0.1:9200',
              'index' = 'mysqlcdc-sql',
              'format' = 'json',
              'json.encode.decimal-as-plain-number' = 'true'
            );
            """;
        // 建表
        tableEnv.executeSql(sinkCreateTable);

        // 同步数据
        tableEnv.executeSql("insert into mysqlcdcEs select * from mysqlcdc");
    }
}
