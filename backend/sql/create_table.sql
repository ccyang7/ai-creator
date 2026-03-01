# 数据库初始化
-- 创建库
create database if not exists ai_creator_db;

-- 切换库
use ai_creator_db;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment '主键ID' primary key,
    user_account varchar(256)                           not null comment '用户账号',
    user_password varchar(512)                          not null comment '用户密码',
    user_name    varchar(256)                           null comment '用户昵称',
    user_avatar  varchar(1024)                          null comment '用户头像',
    user_profile varchar(512)                           null comment '用户简介',
    user_role    varchar(256) default 'user'            not null comment '用户角色：user/admin',
    edit_time    datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    create_time  datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time  datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete    tinyint      default 0                 not null comment '是否删除（0-未删除，1-已删除）',
    UNIQUE KEY uk_user_account (user_account),
    INDEX idx_user_name (user_name)
) comment '用户表' collate = utf8mb4_unicode_ci;
