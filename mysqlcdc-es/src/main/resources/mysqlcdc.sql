create table mysqlcdc(
  id bigint unsigned auto_increment comment '主键',
  name varchar(20) comment '名字',
  age int comment '年龄',
  money decimal(20,2) comment '金额',
  birthday date comment '生日',
  create_time timestamp comment '创建时间',
  update_time datetime comment '修改时间',
  primary key(id)
);