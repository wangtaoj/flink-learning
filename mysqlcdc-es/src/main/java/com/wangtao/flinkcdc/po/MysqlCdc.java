package com.wangtao.flinkcdc.po;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.wangtao.flinkcdc.parser.jackson.FlinkCdcDateDeserialize;
import com.wangtao.flinkcdc.parser.jackson.FlinkCdcDateTimeDeserialize;
import com.wangtao.flinkcdc.parser.jackson.FlinkCdcTimeStampDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author wangtao
 * Created at 2024-08-31
 */
@NoArgsConstructor
@ToString
@Setter
@Getter
public class MysqlCdc {

    private Long id;

    private String name;

    private Integer age;

    private BigDecimal money;

    @JsonDeserialize(using = FlinkCdcDateDeserialize.class)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date birthday;

    @JsonDeserialize(using = FlinkCdcTimeStampDeserialize.class)
    @JsonAlias("create_time")
    private Date createTime;

    @JsonDeserialize(using = FlinkCdcDateTimeDeserialize.class)
    @JsonAlias("update_time")
    private Date updateTime;
}
