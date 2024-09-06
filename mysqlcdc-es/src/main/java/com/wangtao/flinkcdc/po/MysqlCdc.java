package com.wangtao.flinkcdc.po;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.wangtao.flinkcdc.parser.jackson.MysqlDateDeserialize;
import com.wangtao.flinkcdc.parser.jackson.MysqlDateTimeDeserialize;
import com.wangtao.flinkcdc.parser.jackson.MysqlTimestampDeserialize;
import com.wangtao.flinkcdc.util.JsonUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @JsonDeserialize(using = MysqlDateDeserialize.class)
    private LocalDate birthday;

    @JsonDeserialize(using = MysqlTimestampDeserialize.class)
    @JsonFormat(pattern = JsonUtils.STANDARD_PATTERN)
    @JsonAlias("create_time")
    private Instant createTime;

    @JsonDeserialize(using = MysqlDateTimeDeserialize.class)
    @JsonAlias("update_time")
    private LocalDateTime updateTime;
}
