package com.wangtao.flinkcdc;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * @author wangtao
 * Created at 2024-08-29
 */
public class MysqlToEsByFlinkSQL {

    public static void main(String[] args) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 设置 3s 的 checkpoint 间隔
        env.enableCheckpointing(3000);

        TableEnvironment tableEnv = StreamTableEnvironment.create(env);

        String createTable = """
                CREATE TABLE t2 (
                  id INT,
                  a INT,
                  b INT,
                  PRIMARY KEY(id) NOT ENFORCED
                ) WITH (
                  'connector' = 'mysql-cdc',
                  'hostname' = '127.0.0.1',
                  'port' = '3306',
                  'username' = 'root',
                  'password' = '123456',
                  'database-name' = 'test',
                  'table-name' = 't2'
                );
                """.stripIndent();
        // 建表
        tableEnv.executeSql(createTable);
        Table table = tableEnv.sqlQuery("SELECT * FROM t2");
        // 执行并打印
        table.execute().print();
    }
}
